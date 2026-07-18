package com.bmad.todolist.board;

import com.bmad.todolist.auth.UserPrincipal;
import com.bmad.todolist.board.BoardDtos.BoardResponse;
import com.bmad.todolist.board.BoardDtos.BoardSummary;
import com.bmad.todolist.board.BoardDtos.CreateBoardRequest;
import com.bmad.todolist.board.BoardDtos.UpdateBoardRequest;
import com.bmad.todolist.board.BoardDtos.UpdateColumnsRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
@PreAuthorize("hasRole('ADMIN')")
public class BoardController {

	private final BoardService boardService;

	public BoardController(BoardService boardService) {
		this.boardService = boardService;
	}

	@GetMapping
	public List<BoardSummary> list(@AuthenticationPrincipal UserPrincipal principal) {
		return boardService.list(principal);
	}

	@GetMapping("/{id}")
	public BoardResponse get(
			@PathVariable Long id,
			@AuthenticationPrincipal UserPrincipal principal) {
		return boardService.get(id, principal);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BoardResponse create(
			@Valid @RequestBody CreateBoardRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return boardService.create(request, principal);
	}

	@PutMapping("/{id}")
	public BoardResponse rename(
			@PathVariable Long id,
			@Valid @RequestBody UpdateBoardRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return boardService.rename(id, request, principal);
	}

	@PutMapping("/{id}/columns")
	public BoardResponse configureColumns(
			@PathVariable Long id,
			@Valid @RequestBody UpdateColumnsRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return boardService.configureColumns(id, request, principal);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal UserPrincipal principal) {
		boardService.delete(id, principal);
	}
}
