package com.bmad.todolist.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bmad.todolist.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void loginWithValidCredentialsReturnsJwtAndUser() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"admin123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.username").value("admin"))
				.andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
				.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(body.get("accessToken").asString().split("\\.")).hasSize(3);
	}

	@Test
	void loginWithInvalidCredentialsReturnsUnauthorizedWithoutLeakingDetails() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"))
				.andExpect(jsonPath("$.message").value(not(containsString("BCrypt"))))
				.andExpect(jsonPath("$.message").value(not(containsString("admin123"))));
	}

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meWithValidTokenReturnsCurrentUser() throws Exception {
		String token = loginAndGetToken();

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.roles[0]").value("ADMIN"))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void meWithTamperedTokenReturnsUnauthorized() throws Exception {
		String token = loginAndGetToken();
		String tampered = token.substring(0, token.length() - 4) + "xxxx";

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminPasswordIsStoredAsBcryptHash() {
		var admin = userRepository.findByUsername("admin").orElseThrow();
		assertThat(admin.getPasswordHash()).startsWith("$2");
		assertThat(admin.getPasswordHash()).doesNotContain("admin123");
		assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	private String loginAndGetToken() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"admin123"}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asString();
	}
}
