package com.bmad.todolist.autotests.api;

import static io.restassured.RestAssured.given;

import com.bmad.todolist.autotests.config.AutotestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public final class ApiClient {

	private ApiClient() {
	}

	public static RequestSpecification baseRequest() {
		return given()
				.baseUri(AutotestConfig.apiBaseUrl())
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON);
	}

	public static Response health() {
		return baseRequest().when().get("/actuator/health");
	}

	public static Response login(String username, String password) {
		return baseRequest()
				.body("""
						{"username":"%s","password":"%s"}
						""".formatted(username, password))
				.when()
				.post("/api/auth/login");
	}

	public static String loginAsAdmin() {
		Response response = login(AutotestConfig.adminUsername(), AutotestConfig.adminPassword());
		response.then().statusCode(200);
		return response.jsonPath().getString("accessToken");
	}
}
