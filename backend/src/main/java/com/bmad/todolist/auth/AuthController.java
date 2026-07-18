package com.bmad.todolist.auth;

import com.bmad.todolist.user.Role;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		String token = jwtService.generateToken(principal);
		return LoginResponse.bearer(token, toMeResponse(principal));
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		return toMeResponse(principal);
	}

	private static MeResponse toMeResponse(UserPrincipal principal) {
		List<String> roles = principal.getUser().getRoles().stream()
				.map(Role::getName)
				.sorted()
				.toList();
		return new MeResponse(principal.getId(), principal.getUsername(), roles);
	}
}
