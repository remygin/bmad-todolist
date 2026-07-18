package com.bmad.todolist.board;

import com.bmad.todolist.card.CardDtos.CardResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class BoardDtos {

	private BoardDtos() {
	}

	public record BoardSummary(
			Long id,
			String name,
			Long authorId,
			String authorUsername,
			Instant createdAt,
			Instant updatedAt) {
	}

	public record ColumnResponse(
			Long id,
			ColumnStatus status,
			String name,
			int position,
			List<CardResponse> cards) {
	}

	public record BoardResponse(
			Long id,
			String name,
			Long authorId,
			String authorUsername,
			Instant createdAt,
			Instant updatedAt,
			List<ColumnResponse> columns) {
	}

	public record CreateBoardRequest(@NotBlank @Size(max = 120) String name) {
	}

	public record UpdateBoardRequest(@NotBlank @Size(max = 120) String name) {
	}

	public record UpdateColumnItem(
			@NotNull Long id,
			@NotNull ColumnStatus status,
			@NotBlank @Size(max = 80) String name,
			@NotNull Integer position) {
	}

	public record UpdateColumnsRequest(
			@NotEmpty @Size(min = 3, max = 3) List<@Valid UpdateColumnItem> columns) {
	}
}
