package com.bmad.todolist.auth;

import java.util.List;

public record MeResponse(
		Long id,
		String username,
		List<String> roles
) {
}
