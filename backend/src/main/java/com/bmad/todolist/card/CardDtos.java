package com.bmad.todolist.card;

import com.bmad.todolist.board.ColumnStatus;
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
			Instant updatedAt) {
	}

	public record CreateCardRequest(
			@NotBlank @Size(max = 200) String title,
			@Size(max = 4000) String description,
			@NotNull ColumnStatus status) {
	}

	public record UpdateCardRequest(
			@NotBlank @Size(max = 200) String title,
			@Size(max = 4000) String description) {
	}

	public record MoveCardRequest(
			@NotNull ColumnStatus targetStatus,
			@NotNull Integer targetIndex) {
	}
}
