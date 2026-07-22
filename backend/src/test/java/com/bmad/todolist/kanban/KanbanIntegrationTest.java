package com.bmad.todolist.kanban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bmad.todolist.auth.JwtService;
import com.bmad.todolist.auth.UserPrincipal;
import com.bmad.todolist.board.BoardColumnRepository;
import com.bmad.todolist.board.BoardRepository;
import com.bmad.todolist.card.CardRepository;
import com.bmad.todolist.user.RoleRepository;
import com.bmad.todolist.user.User;
import com.bmad.todolist.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class KanbanIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private BoardColumnRepository columnRepository;

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private String adminToken;

	@BeforeEach
	void cleanKanbanData() throws Exception {
		cardRepository.deleteAll();
		columnRepository.deleteAll();
		boardRepository.deleteAll();
		adminToken = loginAndGetToken("admin", "admin123");
	}

	@Test
	void kanbanApiRequiresAdminRole() throws Exception {
		mockMvc.perform(get("/api/boards"))
				.andExpect(status().isUnauthorized());

		User viewer = userRepository.findByUsername("viewer")
				.orElseGet(() -> userRepository.save(new User("viewer", passwordEncoder.encode("password"))));
		String viewerToken = jwtService.generateToken(new UserPrincipal(viewer));

		mockMvc.perform(get("/api/boards").header("Authorization", bearer(viewerToken)))
				.andExpect(status().isForbidden());
	}

	@Test
	void createBoardCreatesExactlyThreeSystemColumnsAndRejectsInvalidNames() throws Exception {
		long boardId = createBoard("Product");

		mockMvc.perform(get("/api/boards/{id}", boardId).header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Product"))
				.andExpect(jsonPath("$.columns.length()").value(3))
				.andExpect(jsonPath("$.columns[0].status").value("TODO"))
				.andExpect(jsonPath("$.columns[0].name").value("Todo"))
				.andExpect(jsonPath("$.columns[1].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.columns[2].status").value("DONE"));

		mockMvc.perform(post("/api/boards")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"   \"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/boards")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"product\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void fullColumnConfigurationPersistsOrderNamesAndStatuses() throws Exception {
		JsonNode board = getBoard(createBoard("Release"));
		JsonNode todo = board.get("columns").get(0);
		JsonNode progress = board.get("columns").get(1);
		JsonNode done = board.get("columns").get(2);

		String body = """
				{"columns":[
				  {"id":%d,"status":"DONE","name":"Shipped","position":0},
				  {"id":%d,"status":"TODO","name":"Backlog","position":1},
				  {"id":%d,"status":"IN_PROGRESS","name":"Building","position":2}
				]}
				""".formatted(done.get("id").asLong(), todo.get("id").asLong(), progress.get("id").asLong());

		mockMvc.perform(put("/api/boards/{id}/columns", board.get("id").asLong())
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/boards/{id}", board.get("id").asLong())
						.header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].status").value("DONE"))
				.andExpect(jsonPath("$.columns[0].name").value("Shipped"))
				.andExpect(jsonPath("$.columns[1].status").value("TODO"))
				.andExpect(jsonPath("$.columns[2].status").value("IN_PROGRESS"));

		String incomplete = """
				{"columns":[
				  {"id":%d,"status":"TODO","name":"One","position":0},
				  {"id":%d,"status":"IN_PROGRESS","name":"Two","position":1}
				]}
				""".formatted(todo.get("id").asLong(), progress.get("id").asLong());
		mockMvc.perform(put("/api/boards/{id}/columns", board.get("id").asLong())
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(incomplete))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cardCrudAndMoveNormalizeStablePositions() throws Exception {
		long boardId = createBoard("Delivery");
		long first = createCard(boardId, "First", "TODO");
		long second = createCard(boardId, "Second", "TODO");
		long active = createCard(boardId, "Active", "IN_PROGRESS");

		mockMvc.perform(put("/api/cards/{id}", first)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"First updated\",\"description\":\"Details\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("First updated"))
				.andExpect(jsonPath("$.description").value("Details"));

		mockMvc.perform(patch("/api/cards/{id}/move", second)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"targetStatus\":\"IN_PROGRESS\",\"targetIndex\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.position").value(0));

		JsonNode reloaded = getBoard(boardId);
		JsonNode todoCards = reloaded.get("columns").get(0).get("cards");
		JsonNode progressCards = reloaded.get("columns").get(1).get("cards");
		assertThat(todoCards.size()).isEqualTo(1);
		assertThat(todoCards.get(0).get("id").asLong()).isEqualTo(first);
		assertThat(todoCards.get(0).get("position").asInt()).isZero();
		assertThat(progressCards.get(0).get("id").asLong()).isEqualTo(second);
		assertThat(progressCards.get(1).get("id").asLong()).isEqualTo(active);
		assertThat(progressCards.get(0).get("position").asInt()).isZero();
		assertThat(progressCards.get(1).get("position").asInt()).isEqualTo(1);

		mockMvc.perform(delete("/api/cards/{id}", second).header("Authorization", bearer(adminToken)))
				.andExpect(status().isNoContent());
		JsonNode afterDelete = getBoard(boardId).get("columns").get(1).get("cards");
		assertThat(afterDelete.size()).isEqualTo(1);
		assertThat(afterDelete.get(0).get("id").asLong()).isEqualTo(active);
		assertThat(afterDelete.get(0).get("position").asInt()).isZero();
	}

	@Test
	void invalidMoveDoesNotChangePersistedOrder() throws Exception {
		long boardId = createBoard("Atomic");
		long first = createCard(boardId, "First", "TODO");
		long second = createCard(boardId, "Second", "TODO");

		mockMvc.perform(patch("/api/cards/{id}/move", first)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"targetStatus\":\"DONE\",\"targetIndex\":2}"))
				.andExpect(status().isBadRequest());

		JsonNode cards = getBoard(boardId).get("columns").get(0).get("cards");
		assertThat(cards.get(0).get("id").asLong()).isEqualTo(first);
		assertThat(cards.get(1).get("id").asLong()).isEqualTo(second);
	}

	@Test
	void deletingBoardCascadesColumnsAndCards() throws Exception {
		long boardId = createBoard("Disposable");
		long cardId = createCard(boardId, "Remove me", "TODO");

		mockMvc.perform(delete("/api/boards/{id}", boardId).header("Authorization", bearer(adminToken)))
				.andExpect(status().isNoContent());

		assertThat(boardRepository.findById(boardId)).isEmpty();
		assertThat(columnRepository.findAllByBoardIdOrderByPosition(boardId)).isEmpty();
		assertThat(cardRepository.findById(cardId)).isEmpty();
		mockMvc.perform(get("/api/boards/{id}", boardId).header("Authorization", bearer(adminToken)))
				.andExpect(status().isNotFound());
	}

	@Test
	void resourcesOwnedByAnotherAdminAreHidden() throws Exception {
		long boardId = createBoard("Private");
		long cardId = createCard(boardId, "Secret", "TODO");
		User other = userRepository.findByUsername("other-admin").orElseGet(() -> {
			User user = new User("other-admin", passwordEncoder.encode("password"));
			user.addRole(roleRepository.findByName("ADMIN").orElseThrow());
			return userRepository.save(user);
		});
		String otherToken = jwtService.generateToken(new UserPrincipal(other));

		mockMvc.perform(get("/api/boards/{id}", boardId).header("Authorization", bearer(otherToken)))
				.andExpect(status().isNotFound());
		mockMvc.perform(put("/api/cards/{id}", cardId)
						.header("Authorization", bearer(otherToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Stolen\",\"description\":null}"))
				.andExpect(status().isNotFound());
	}

	private long createBoard(String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/boards")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(java.util.Map.of("name", name))))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private long createCard(long boardId, String title, String status) throws Exception {
		String body = objectMapper.writeValueAsString(java.util.Map.of(
				"title", title,
				"description", "",
				"status", status
		));
		MvcResult result = mockMvc.perform(post("/api/boards/{id}/cards", boardId)
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
	}

	private JsonNode getBoard(long boardId) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/boards/{id}", boardId)
						.header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String loginAndGetToken(String username, String password) throws Exception {
		String body = objectMapper.writeValueAsString(java.util.Map.of(
				"username", username,
				"password", password
		));
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.get("accessToken")
				.asString();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
