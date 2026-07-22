package com.bmad.todolist.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@SpringBootTest
@ActiveProfiles("test")
class V3MigrationIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_creatorIdColumnExistsAndNotNull() {
		List<Map<String, Object>> cols = jdbc.queryForList(
				"SELECT column_name, data_type, is_nullable " +
				"FROM information_schema.columns " +
				"WHERE table_name = 'cards' " +
				"AND column_name = 'creator_id'");
		assertThat(cols).hasSize(1);
		assertThat(cols.get(0).get("is_nullable")).isEqualTo("NO");
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_assigneeIdColumnExistsAndNullable() {
		List<Map<String, Object>> cols = jdbc.queryForList(
				"SELECT column_name, data_type, is_nullable " +
				"FROM information_schema.columns " +
				"WHERE table_name = 'cards' " +
				"AND column_name = 'assignee_id'");
		assertThat(cols).hasSize(1);
		assertThat(cols.get(0).get("is_nullable")).isEqualTo("YES");
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_creatorIdFkExists() {
		List<Map<String, Object>> fks = jdbc.queryForList(
				"SELECT constraint_name FROM information_schema.table_constraints " +
				"WHERE table_name = 'cards' AND constraint_type = 'FOREIGN KEY' " +
				"AND constraint_name = 'fk_cards_creator'");
		assertThat(fks).hasSize(1);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_assigneeIdFkExists() {
		List<Map<String, Object>> fks = jdbc.queryForList(
				"SELECT constraint_name FROM information_schema.table_constraints " +
				"WHERE table_name = 'cards' AND constraint_type = 'FOREIGN KEY' " +
				"AND constraint_name = 'fk_cards_assignee'");
		assertThat(fks).hasSize(1);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_backfillCreatorIdEqualsBoardAuthor() {
		List<Map<String, Object>> mismatches = jdbc.queryForList(
				"SELECT c.id FROM cards c " +
				"JOIN boards b ON c.board_id = b.id " +
				"WHERE c.creator_id != b.author_id OR c.creator_id IS NULL");
		assertThat(mismatches).isEmpty();
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_creatorIdIndexExists() {
		List<Map<String, Object>> indexes = jdbc.queryForList(
				"SELECT index_name FROM information_schema.indexes " +
				"WHERE table_name = 'cards' AND index_name = 'idx_cards_creator_id'");
		assertThat(indexes).hasSize(1);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_assigneeIdIndexExists() {
		List<Map<String, Object>> indexes = jdbc.queryForList(
				"SELECT index_name FROM information_schema.indexes " +
				"WHERE table_name = 'cards' AND index_name = 'idx_cards_assignee_id'");
		assertThat(indexes).hasSize(1);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_backfillUpdateRunsCorrectly() {
		jdbc.update("UPDATE cards SET creator_id = 10");
		jdbc.update(
				"UPDATE cards c SET creator_id = b.author_id FROM boards b WHERE c.board_id = b.id");
		List<Map<String, Object>> mismatches = jdbc.queryForList(
				"SELECT c.id FROM cards c " +
				"JOIN boards b ON c.board_id = b.id " +
				"WHERE c.creator_id != b.author_id OR c.creator_id IS NULL");
		assertThat(mismatches).isEmpty();
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_notNullViolationOnCreatorId() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO cards (title, board_id, status, position, created_at, updated_at, assignee_id) " +
				"VALUES ('Test', 10, 'TODO', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_fkViolationOnCreatorId() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO cards (title, board_id, status, position, creator_id, created_at, updated_at) " +
				"VALUES ('Test', 10, 'TODO', 5, 99999, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_fkViolationOnAssigneeId() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO cards (title, board_id, status, position, creator_id, assignee_id, created_at, updated_at) " +
				"VALUES ('Test', 10, 'TODO', 5, 10, 99999, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@Sql(scripts = "/sql/seed-pre-v3.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
	void v3Migration_assigneeIdNullableInsertWorks() {
		jdbc.update(
				"INSERT INTO cards (id, title, board_id, status, position, creator_id, created_at, updated_at) " +
				"VALUES (100, 'Nullable test', 10, 'TODO', 5, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
		int count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM cards WHERE title = 'Nullable test' AND assignee_id IS NULL",
				Integer.class);
		assertThat(count).isOne();
	}
}
