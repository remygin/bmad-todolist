package com.bmad.todolist.card;

import com.bmad.todolist.board.ColumnStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CardDtos {

	private CardDtos() {
	}

	public record CardResponse(
			Long id,
			Long boardId,
			String title,
			String description,
			ColumnStatus status,
			int position,
			Instant createdAt,
			Instant updatedAt,
			Long creatorId,
			String creatorUsername,
			Long assigneeId,
			String assigneeUsername) {

		public static CardResponse from(Card card) {
			return new CardResponse(
					card.getId(),
					card.getBoard().getId(),
					card.getTitle(),
					card.getDescription(),
					card.getStatus(),
					card.getPosition(),
					card.getCreatedAt(),
					card.getUpdatedAt(),
					card.getCreator().getId(),
					card.getCreator().getUsername(),
					card.getAssignee() != null ? card.getAssignee().getId() : null,
					card.getAssignee() != null ? card.getAssignee().getUsername() : null
			);
		}
	}

	public record CreateCardRequest(
			@NotBlank @Size(max = 200) String title,
			@Size(max = 4000) String description,
			@NotNull ColumnStatus status,
			@Nullable Long assigneeId) {
	}

	public record UpdateCardRequest(
			@NotBlank @Size(max = 200) String title,
			@Size(max = 4000) String description,
			@Nullable Long assigneeId,
			@JsonProperty(defaultValue = "false") boolean resetAssignee) {
	}

	public record MoveCardRequest(
			@NotNull ColumnStatus targetStatus,
			@NotNull Integer targetIndex) {
	}
}
