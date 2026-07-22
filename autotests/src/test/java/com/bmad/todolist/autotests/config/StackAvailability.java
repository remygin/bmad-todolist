package com.bmad.todolist.autotests.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class StackAvailability {

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(2))
			.build();

	private StackAvailability() {
	}

	public static boolean apiReachable() {
		return reachable(AutotestConfig.apiBaseUrl() + "/actuator/health");
	}

	public static boolean uiReachable() {
		return reachable(AutotestConfig.uiBaseUrl());
	}

	private static boolean reachable(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(3))
					.GET()
					.build();
			HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
			return response.statusCode() > 0 && response.statusCode() < 500;
		}
		catch (Exception e) {
			return false;
		}
	}
}
