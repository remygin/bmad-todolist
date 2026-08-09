package com.bmad.todolist.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bmad.todolist.autotests.api.ApiClient;
import com.bmad.todolist.autotests.config.AutotestConfig;
import com.bmad.todolist.autotests.config.StackAvailability;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HealthApiTest {

	@BeforeEach
	void requireApi() {
		assumeTrue(StackAvailability.apiReachable(),
				() -> "API недоступен: поднимите backend (" + AutotestConfig.apiBaseUrl() + ")");
	}

	@Test
	void healthIsUp() {
		Response response = ApiClient.health();

		response.then().statusCode(200);
		assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
	}
}
