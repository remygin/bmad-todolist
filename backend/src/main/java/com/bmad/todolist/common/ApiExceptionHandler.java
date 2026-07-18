package com.bmad.todolist.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(
			BadCredentialsException ex,
			HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid username or password", request, List.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthentication(
			AuthenticationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required", request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed", request, details);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(
			BadRequestException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid request parameter", request, List.of());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			ResourceNotFoundException ex,
			HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(
			ConflictException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(
			DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Conflict", "Resource already exists", request, List.of());
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status,
			String error,
			String message,
			HttpServletRequest request,
			List<String> details) {
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				status.value(),
				error,
				message,
				request.getRequestURI(),
				details
		);
		return ResponseEntity.status(status).body(body);
	}
}
