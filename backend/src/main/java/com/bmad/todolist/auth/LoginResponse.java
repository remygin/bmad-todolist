package com.bmad.todolist.auth;

public record LoginResponse(
		String accessToken,
		String tokenType,
		MeResponse user
) {

	public static LoginResponse bearer(String token, MeResponse user) {
		return new LoginResponse(token, "Bearer", user);
	}
}
