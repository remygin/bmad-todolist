package com.bmad.todolist.card;

import com.bmad.todolist.auth.UserPrincipal;
import com.bmad.todolist.board.Board;
import com.bmad.todolist.board.BoardRepository;
import com.bmad.todolist.board.ColumnStatus;
import com.bmad.todolist.card.CardDtos.CardResponse;
import com.bmad.todolist.card.CardDtos.CreateCardRequest;
import com.bmad.todolist.card.CardDtos.MoveCardRequest;
import com.bmad.todolist.card.CardDtos.UpdateCardRequest;
import com.bmad.todolist.common.BadRequestException;
import com.bmad.todolist.common.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

	private final CardRepository cardRepository;
	private final BoardRepository boardRepository;

	public CardService(CardRepository cardRepository, BoardRepository boardRepository) {
		this.cardRepository = cardRepository;
		this.boardRepository = boardRepository;
	}

	@Transactional
	public CardResponse create(Long boardId, CreateCardRequest request, UserPrincipal principal) {
		Board board = findOwnedBoardForUpdate(boardId, principal.getId());
		int position = Math.toIntExact(cardRepository.countByBoardIdAndStatus(boardId, request.status()));
		Card card = cardRepository.save(new Card(
				board,
				normalizeTitle(request.title()),
				normalizeDescription(request.description()),
				request.status(),
				position
		));
		return toResponse(card);
	}

	@Transactional
	public CardResponse update(Long id, UpdateCardRequest request, UserPrincipal principal) {
		Long boardId = findOwnedBoardId(id, principal.getId());
		findOwnedBoardForUpdate(boardId, principal.getId());
		Card card = findOwnedCard(id, principal.getId());
		card.update(normalizeTitle(request.title()), normalizeDescription(request.description()));
		return toResponse(card);
	}

	@Transactional
	public CardResponse move(Long id, MoveCardRequest request, UserPrincipal principal) {
		Long boardId = findOwnedBoardId(id, principal.getId());
		findOwnedBoardForUpdate(boardId, principal.getId());
		Card card = findOwnedCard(id, principal.getId());

		ColumnStatus sourceStatus = card.getStatus();
		ColumnStatus targetStatus = request.targetStatus();
		List<Card> source = new ArrayList<>(
				cardRepository.findAllByBoardIdAndStatusOrderByPosition(boardId, sourceStatus));
		source.removeIf(item -> item.getId().equals(card.getId()));

		if (sourceStatus == targetStatus) {
			validateTargetIndex(request.targetIndex(), source.size());
			source.add(request.targetIndex(), card);
			normalize(source, targetStatus);
			cardRepository.saveAll(source);
		} else {
			List<Card> target = new ArrayList<>(
					cardRepository.findAllByBoardIdAndStatusOrderByPosition(boardId, targetStatus));
			validateTargetIndex(request.targetIndex(), target.size());
			target.add(request.targetIndex(), card);
			normalize(source, sourceStatus);
			normalize(target, targetStatus);
			cardRepository.saveAll(source);
			cardRepository.saveAll(target);
		}
		return toResponse(card);
	}

	@Transactional
	public void delete(Long id, UserPrincipal principal) {
		Long boardId = findOwnedBoardId(id, principal.getId());
		findOwnedBoardForUpdate(boardId, principal.getId());
		Card card = findOwnedCard(id, principal.getId());
		ColumnStatus status = card.getStatus();
		cardRepository.delete(card);
		cardRepository.flush();

		List<Card> remaining = cardRepository.findAllByBoardIdAndStatusOrderByPosition(boardId, status);
		normalize(remaining, status);
		cardRepository.saveAll(remaining);
	}

	private Card findOwnedCard(Long id, Long authorId) {
		return cardRepository.findByIdAndBoardAuthorId(id, authorId)
				.orElseThrow(() -> new ResourceNotFoundException("Card not found"));
	}

	private Long findOwnedBoardId(Long cardId, Long authorId) {
		return cardRepository.findOwnedBoardId(cardId, authorId)
				.orElseThrow(() -> new ResourceNotFoundException("Card not found"));
	}

	private Board findOwnedBoardForUpdate(Long id, Long authorId) {
		return boardRepository.findOwnedByIdForUpdate(id, authorId)
				.orElseThrow(() -> new ResourceNotFoundException("Board not found"));
	}

	private void validateTargetIndex(int index, int maximum) {
		if (index < 0 || index > maximum) {
			throw new BadRequestException("Target index is outside the target column");
		}
	}

	private void normalize(List<Card> cards, ColumnStatus status) {
		for (int index = 0; index < cards.size(); index++) {
			cards.get(index).move(status, index);
		}
	}

	private String normalizeTitle(String title) {
		String normalized = title.trim();
		if (normalized.isEmpty()) {
			throw new BadRequestException("Card title must not be blank");
		}
		return normalized;
	}

	private String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}

	private CardResponse toResponse(Card card) {
		return new CardResponse(
				card.getId(),
				card.getBoard().getId(),
				card.getTitle(),
				card.getDescription(),
				card.getStatus(),
				card.getPosition(),
				card.getCreatedAt(),
				card.getUpdatedAt()
		);
	}
}
