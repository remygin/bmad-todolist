package com.bmad.todolist.card;

import com.bmad.todolist.auth.UserPrincipal;
import com.bmad.todolist.card.CardDtos.CardResponse;
import com.bmad.todolist.card.CardDtos.CreateCardRequest;
import com.bmad.todolist.card.CardDtos.MoveCardRequest;
import com.bmad.todolist.card.CardDtos.UpdateCardRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class CardController {

	private final CardService cardService;

	public CardController(CardService cardService) {
		this.cardService = cardService;
	}

	@PostMapping("/boards/{boardId}/cards")
	@ResponseStatus(HttpStatus.CREATED)
	public CardResponse create(
			@PathVariable Long boardId,
			@Valid @RequestBody CreateCardRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return cardService.create(boardId, request, principal);
	}

	@PutMapping("/cards/{id}")
	public CardResponse update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateCardRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return cardService.update(id, request, principal);
	}

	@PatchMapping("/cards/{id}/move")
	public CardResponse move(
			@PathVariable Long id,
			@Valid @RequestBody MoveCardRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return cardService.move(id, request, principal);
	}

	@DeleteMapping("/cards/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal UserPrincipal principal) {
		cardService.delete(id, principal);
	}
}
