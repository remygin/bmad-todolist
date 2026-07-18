package com.bmad.todolist.board;

import com.bmad.todolist.auth.UserPrincipal;
import com.bmad.todolist.board.BoardDtos.BoardResponse;
import com.bmad.todolist.board.BoardDtos.BoardSummary;
import com.bmad.todolist.board.BoardDtos.ColumnResponse;
import com.bmad.todolist.board.BoardDtos.CreateBoardRequest;
import com.bmad.todolist.board.BoardDtos.UpdateBoardRequest;
import com.bmad.todolist.board.BoardDtos.UpdateColumnItem;
import com.bmad.todolist.board.BoardDtos.UpdateColumnsRequest;
import com.bmad.todolist.card.Card;
import com.bmad.todolist.card.CardDtos.CardResponse;
import com.bmad.todolist.card.CardRepository;
import com.bmad.todolist.common.BadRequestException;
import com.bmad.todolist.common.ConflictException;
import com.bmad.todolist.common.ResourceNotFoundException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

	private final BoardRepository boardRepository;
	private final BoardColumnRepository columnRepository;
	private final CardRepository cardRepository;

	public BoardService(
			BoardRepository boardRepository,
			BoardColumnRepository columnRepository,
			CardRepository cardRepository) {
		this.boardRepository = boardRepository;
		this.columnRepository = columnRepository;
		this.cardRepository = cardRepository;
	}

	@Transactional(readOnly = true)
	public List<BoardSummary> list(UserPrincipal principal) {
		return boardRepository.findAllByAuthorIdOrderById(principal.getId()).stream()
				.map(this::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public BoardResponse get(Long id, UserPrincipal principal) {
		Board board = findOwned(id, principal.getId());
		return toResponse(board);
	}

	@Transactional
	public BoardResponse create(CreateBoardRequest request, UserPrincipal principal) {
		String name = normalizeName(request.name());
		if (boardRepository.existsByAuthorIdAndNameIgnoreCase(principal.getId(), name)) {
			throw new ConflictException("A board with this name already exists");
		}

		Board board = boardRepository.save(new Board(name, principal.getUser()));
		columnRepository.saveAll(List.of(
				new BoardColumn(board, ColumnStatus.TODO, "Todo", 0),
				new BoardColumn(board, ColumnStatus.IN_PROGRESS, "In progress", 1),
				new BoardColumn(board, ColumnStatus.DONE, "Done", 2)
		));
		return toResponse(board);
	}

	@Transactional
	public BoardResponse rename(Long id, UpdateBoardRequest request, UserPrincipal principal) {
		Board board = findOwnedForUpdate(id, principal.getId());
		String name = normalizeName(request.name());
		if (boardRepository.existsByAuthorIdAndNameIgnoreCaseAndIdNot(principal.getId(), name, id)) {
			throw new ConflictException("A board with this name already exists");
		}
		board.rename(name);
		return toResponse(board);
	}

	@Transactional
	public BoardResponse configureColumns(
			Long id,
			UpdateColumnsRequest request,
			UserPrincipal principal) {
		Board board = findOwnedForUpdate(id, principal.getId());
		List<BoardColumn> columns = columnRepository.findAllByBoardIdOrderByPosition(id);
		validateColumnConfiguration(columns, request.columns());

		Map<Long, UpdateColumnItem> itemsById = new HashMap<>();
		request.columns().forEach(item -> itemsById.put(item.id(), item));
		for (BoardColumn column : columns) {
			UpdateColumnItem item = itemsById.get(column.getId());
			column.configure(item.name().trim(), item.position());
		}
		columnRepository.saveAll(columns);
		return toResponse(board);
	}

	@Transactional
	public void delete(Long id, UserPrincipal principal) {
		Board board = findOwnedForUpdate(id, principal.getId());
		boardRepository.delete(board);
	}

	@Transactional(readOnly = true)
	public Board findOwned(Long id, Long authorId) {
		return boardRepository.findByIdAndAuthorId(id, authorId)
				.orElseThrow(() -> new ResourceNotFoundException("Board not found"));
	}

	@Transactional
	public Board findOwnedForUpdate(Long id, Long authorId) {
		return boardRepository.findOwnedByIdForUpdate(id, authorId)
				.orElseThrow(() -> new ResourceNotFoundException("Board not found"));
	}

	private void validateColumnConfiguration(
			List<BoardColumn> current,
			List<UpdateColumnItem> requested) {
		if (current.size() != 3 || requested.size() != 3) {
			throw new BadRequestException("Exactly three columns are required");
		}

		Set<Long> currentIds = current.stream().map(BoardColumn::getId).collect(java.util.stream.Collectors.toSet());
		Set<Long> requestedIds = new HashSet<>();
		Set<ColumnStatus> statuses = EnumSet.noneOf(ColumnStatus.class);
		Set<Integer> positions = new HashSet<>();
		for (UpdateColumnItem item : requested) {
			if (!requestedIds.add(item.id()) || !statuses.add(item.status()) || !positions.add(item.position())) {
				throw new BadRequestException("Column IDs, statuses and positions must be unique");
			}
			if (item.position() < 0 || item.position() > 2) {
				throw new BadRequestException("Column positions must be 0, 1 and 2");
			}
		}
		if (!currentIds.equals(requestedIds)
				|| !statuses.equals(EnumSet.allOf(ColumnStatus.class))
				|| !positions.equals(Set.of(0, 1, 2))) {
			throw new BadRequestException("The complete set of system columns is required");
		}

		Map<Long, ColumnStatus> currentStatuses = new HashMap<>();
		current.forEach(column -> currentStatuses.put(column.getId(), column.getStatus()));
		if (requested.stream().anyMatch(item -> currentStatuses.get(item.id()) != item.status())) {
			throw new BadRequestException("A column system status cannot be changed");
		}
	}

	private String normalizeName(String name) {
		String normalized = name.trim();
		if (normalized.isEmpty()) {
			throw new BadRequestException("Board name must not be blank");
		}
		return normalized;
	}

	private BoardSummary toSummary(Board board) {
		return new BoardSummary(
				board.getId(),
				board.getName(),
				board.getAuthor().getId(),
				board.getAuthor().getUsername(),
				board.getCreatedAt(),
				board.getUpdatedAt()
		);
	}

	private BoardResponse toResponse(Board board) {
		Map<ColumnStatus, List<CardResponse>> cardsByStatus = cardRepository
				.findAllByBoardIdOrderByStatusAscPositionAsc(board.getId())
				.stream()
				.map(this::toCardResponse)
				.collect(java.util.stream.Collectors.groupingBy(CardResponse::status));

		List<ColumnResponse> columns = columnRepository.findAllByBoardIdOrderByPosition(board.getId()).stream()
				.map(column -> new ColumnResponse(
						column.getId(),
						column.getStatus(),
						column.getDisplayName(),
						column.getPosition(),
						cardsByStatus.getOrDefault(column.getStatus(), List.of())
				))
				.toList();

		return new BoardResponse(
				board.getId(),
				board.getName(),
				board.getAuthor().getId(),
				board.getAuthor().getUsername(),
				board.getCreatedAt(),
				board.getUpdatedAt(),
				columns
		);
	}

	private CardResponse toCardResponse(Card card) {
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
