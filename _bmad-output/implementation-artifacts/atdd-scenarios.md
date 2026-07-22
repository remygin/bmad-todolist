---
id: "atdd-scenarios-4.1"
story: "4.1"
epic: 4
title: "V3 миграция — добавление creator_id и assignee_id в таблицу cards"
workflow: "ai-prepare-atdd"
generated: "2026-07-22"
author: "Murat (Master Test Architect) via bmad-tea@1.0"
status: "draft"
related:
  - "_bmad-output/test-artifacts/test-design-qa.md"
  - "_bmad-output/test-artifacts/test-design-architecture.md"
  - "_bmad-output/implementation-artifacts/brownfield-handoff.yaml"
---

# ATDD Scenarios: V3 миграция — добавление creator_id и assignee_id в таблицу cards

## 1. Scope и границы

**Story 4.1** — только Flyway V3 миграция базы данных. Никаких изменений Java-кода, entity, DTO, сервисов или фронтенда.

**Что тестируем:**
- Применение `V3__add_card_users.sql` на чистой БД
- Backfill существующих карточек: `creator_id = board.author_id`
- Ограничения: NOT NULL, FK, индексы
- Совместимость с H2 (MODE=PostgreSQL) в профиле `test`

**Что НЕ тестируем (в рамках этой story):**
- Java-entity Card (поля появятся в следующей story)
- API-эндпоинты createCard/updateCard/Board detail
- FE отображение creator/assignee
- Производительность миграции

---

## 2. Карта рисков (Risk → Acceptance Criteria → Test)

| Risk ID | Risk | Score | AC | Test ID | Приоритет |
|---------|------|-------|----|---------|-----------|
| R-07 | V3 backfill + NOT NULL | 6 | AC-1, AC-2, AC-3 | MIG-01…MIG-06 | P1 |
| G-04 | Ограничения и индексы | — | AC-3 | MIG-03, MIG-04 | P1 |

---

## 3. Acceptance Scenarios

### 3.1 Миграция на чистой БД (AC-1)

#### Scenario 3.1.1 — Чистое применение миграции (Positive)

```
Given: чистая БД без полей creator_id/assignee_id
  And: таблица cards существует (V1/V2 применены)
  And: Board.author_id NOT NULL для всех записей
When: Flyway применяет V3__add_card_users.sql
Then: в таблице cards существуют колонки:
  - creator_id BIGINT NOT NULL
  - assignee_id BIGINT nullable
  And: обе колонки имеют FK → users(id)
  And: существуют индексы idx_cards_creator_id и idx_cards_assignee_id
```

**Пример проверки (H2 MODE=PostgreSQL):**
```sql
SELECT column_name, data_type, is_nullable
  FROM information_schema.columns
  WHERE table_name = 'cards'
    AND column_name IN ('creator_id', 'assignee_id');
-- Ожидаемый результат:
-- creator_id | BIGINT | NO
-- assignee_id | BIGINT | YES

SELECT constraint_name, constraint_type
  FROM information_schema.table_constraints
  WHERE table_name = 'cards'
    AND constraint_type = 'FOREIGN KEY';
-- 2 FK: fk_cards_creator, fk_cards_assignee

SELECT index_name FROM information_schema.indexes
  WHERE table_name = 'cards'
    AND index_name IN ('idx_cards_creator_id', 'idx_cards_assignee_id');
-- 2 индекса
```

#### Scenario 3.1.2 — Чистая БД без карточек (Edge)

```
Given: чистая БД с таблицами V1/V2
  And: в таблице cards нет записей
When: применяется V3
Then: creator_id добавлен как NOT NULL
  And: assignee_id добавлен как nullable
  And: миграция не падает (пустой backfill корректен)
```

---

### 3.2 Backfill существующих карточек (AC-2)

#### Scenario 3.2.1 — Backfill с одной доской (Positive)

```
Given: в БД есть карточки, созданные до V3
  And: все карточки принадлежат board с board.author_id = 1
  And: board.author_id IS NOT NULL
When: миграция V3 выполнена
Then: creator_id всех карточек = board.author_id (= 1)
  And: assignee_id всех карточек = NULL (не был задан)
```

**Пример (SQL-проверка):**
```sql
SELECT c.id, c.creator_id, b.author_id
  FROM cards c
  JOIN boards b ON c.board_id = b.id;
-- Ожидание: c.creator_id = b.author_id для каждой строки
```

#### Scenario 3.2.2 — Backfill с несколькими досками (Positive)

```
Given: есть 3 доски (board.author_id = 1, 5, 10)
  And: на каждой доске по 2+ карточки
When: миграция V3 выполнена
Then: creator_id каждой карточки = board.author_id её доски
  And: нет ни одной карточки с creator_id IS NULL
```

**Пример данных:**
```yaml
boards:
  - id: 1, author_id: 1, cards: [10, 11]
  - id: 2, author_id: 5, cards: [20, 21, 22]
  - id: 3, author_id: 10, cards: [30]
Ожидание:
  card[10].creator_id = 1
  card[20].creator_id = 5
  card[30].creator_id = 10
```

#### Scenario 3.2.3 — Backfill при пустом board (Edge — невозможно по assumption A-3)

```
Given: assumption A-3 гласит Board.author NOT NULL
  And: A-3 подтверждён (все board имеют author_id)
When: миграция V3 применена
Then: backfill корректен
  And: SET NOT NULL не падает
```

> Если A-3 нарушен (board без author_id), SET NOT NULL упадёт с `NULL value` violation. Это ожидаемое поведение, предотвращающее повреждение данных.

---

### 3.3 Ограничения и индексы (AC-3)

#### Scenario 3.3.1 — FK creator_id → users(id) (Positive)

```
Given: V3 миграция применена
When: попытка INSERT карточки с creator_id = несуществующий user.id
Then: constraint violation (FK)
```

```sql
INSERT INTO cards (title, column_id, board_id, creator_id, position)
  VALUES ('Test', 1, 1, 99999, 1);
-- ERROR: Referential integrity constraint violation
```

#### Scenario 3.3.2 — FK assignee_id → users(id) (Positive)

```
Given: V3 миграция применена
When: попытка INSERT с assignee_id = несуществующий user.id
Then: constraint violation (FK)
```

#### Scenario 3.3.3 — NOT NULL violation on creator_id (Negative)

```
Given: V3 миграция применена
When: попытка INSERT карточки без creator_id (NULL)
Then: NOT NULL constraint violation
```

```sql
INSERT INTO cards (title, column_id, board_id, creator_id, position)
  VALUES ('Test', 1, 1, NULL, 1);
-- ERROR: NULL not allowed for column CREATOR_ID
```

#### Scenario 3.3.4 — assignee_id может быть NULL (Positive)

```
Given: V3 миграция применена
When: INSERT карточки с assignee_id = NULL
Then: запись успешна, assignee_id = NULL
```

---

### 3.4 Интеграционные сценарии (системный уровень)

#### Scenario 3.4.1 — Влияние V3 на существующие V1/V2 миграции (Regression)

```
Given: V1__init.sql и V2__add_boards.sql применены
When: применяется V3__add_card_users.sql
Then: V1/V2 не модифицированы
  And: schema_version содержит 3 записи (V1, V2, V3)
  And: checksum V1 и V2 не изменились
```

#### Scenario 3.4.2 — Идемпотентность повторного применения (Resilience)

```
Given: V3 миграция уже применена
When: Flyway пытается применить V3 повторно
Then: Flyway пропускает (V3 уже в schema_version)
  And: ошибки нет
```

#### Scenario 3.4.3 — Совместимость с H2 MODE=PostgreSQL (Infrastructure)

```
Given: профиль test (H2 MODE=PostgreSQL)
  And: V1/V2 применены
When: выполняется V3__add_card_users.sql
Then: SQL не вызывает ошибок H2
  And: все конструкции (ALTER TABLE, UPDATE с FROM, SET NOT NULL, ADD CONSTRAINT, CREATE INDEX) работают
```

---

## 4. Классификация по приоритетам

### P0 (Critical) — нет для этой story (scope только миграция)

### P1 (High) — обязательны к выполнению

| ID | Scenario | Проверка | Risk |
|----|----------|----------|------|
| MIG-01 | 3.1.1 — Чистое применение миграции | columns, FK, indexes | R-07 |
| MIG-02 | 3.2.1 — Backfill одной доски | creator_id = board.author_id | R-07 |
| MIG-03 | 3.3.1 — FK creator_id | constraint violation | G-04 |
| MIG-04 | 3.3.3 — NOT NULL creator_id | constraint violation | G-04 |
| MIG-05 | 3.4.1 — Regression V1/V2 | checksums unchanged | R-10 |
| MIG-06 | 3.4.3 — H2 совместимость | no SQL errors | — |

### P2 (Medium) — желательные

| ID | Scenario | Проверка |
|----|----------|----------|
| MIG-07 | 3.1.2 — Пустая БД | миграция не падает |
| MIG-08 | 3.2.2 — Backfill нескольких досок | корректность per-board |
| MIG-09 | 3.3.2 — FK assignee_id | constraint violation |
| MIG-10 | 3.3.4 — assignee_id nullable | успешный INSERT |

### P3 (Low) — дополнительные

| ID | Scenario | Проверка |
|----|----------|----------|
| MIG-11 | 3.4.2 — Идемпотентность | Flyway skip |
| MIG-12 | V3 rollback script (V3.1) | revert корректен |

---

## 5. Критерии прохождения

### Pass Criteria (все P1 проходят)

1. **MIG-01**: После V3 в `cards` есть `creator_id BIGINT NOT NULL`, `assignee_id BIGINT nullable`, 2 FK, 2 индекса
2. **MIG-02**: Все существующие карточки имеют `creator_id = board.author_id`
3. **MIG-03**: INSERT с несуществующим `creator_id` → FK violation
4. **MIG-04**: INSERT с `creator_id = NULL` → NOT NULL violation
5. **MIG-05**: V1/V2 checksums не изменились после V3
6. **MIG-06**: V3 SQL совместим с H2 MODE=PostgreSQL

### Fail Criteria

- Любой P1 тест не проходит → миграция не готова к PR
- SET NOT NULL падает на существующих данных → повреждённые данные в production
- FK не созданы → потеря referential integrity

---

## 6. Матрица трассировки (Story → AC → Test)

| Story AC | Test ID | Scenario | Статус |
|----------|---------|----------|--------|
| AC-1 (P0): Чистая миграция | MIG-01 | 3.1.1 — columns/FK/indexes | planned |
| AC-1 (P0): Чистая миграция | MIG-06 | 3.4.3 — H2 совместимость | planned |
| AC-2 (P1-005, P0-006 → R-07): Backfill | MIG-02 | 3.2.1 — backfill одной доски | planned |
| AC-2 (P1-005, P0-006 → R-07): Backfill | MIG-08 | 3.2.2 — backfill нескольких досок | planned |
| AC-3 (G-04): Ограничения и индексы | MIG-03 | 3.3.1 — FK creator_id | planned |
| AC-3 (G-04): Ограничения и индексы | MIG-04 | 3.3.3 — NOT NULL creator_id | planned |
| AC-3 (G-04): Ограничения и индексы | MIG-09 | 3.3.2 — FK assignee_id | planned |
| AC-3 (G-04): Ограничения и индексы | MIG-10 | 3.3.4 — assignee_id nullable | planned |
| — | MIG-05 | 3.4.1 — Regression V1/V2 | planned |
| — | MIG-07 | 3.1.2 — Пустая БД | planned |
| — | MIG-11 | 3.4.2 — Идемпотентность | planned |

---

## 7. Тестовые данные (Data Factories)

### Seed-данные для миграционных тестов

```sql
-- V1: users
INSERT INTO users (id, username, password, role)
VALUES (1, 'admin', '$2a$10...', 'ADMIN'),
       (2, 'user2', '$2a$10...', 'ADMIN');

-- V2: boards
INSERT INTO boards (id, title, author_id)
VALUES (1, 'Board 1', 1),
       (2, 'Board 2', 2);

-- V2: columns
INSERT INTO columns (id, title, board_id, status, position)
VALUES (1, 'To Do', 1, 'TODO', 0),
       (2, 'In Progress', 1, 'IN_PROGRESS', 1),
       (3, 'To Do', 2, 'TODO', 0);

-- V2: cards (pre-V3 — БЕЗ creator_id/assignee_id)
INSERT INTO cards (id, title, column_id, board_id, position)
VALUES (10, 'Card on Board 1', 1, 1, 0),
       (11, 'Second on Board 1', 2, 1, 0),
       (20, 'Card on Board 2', 3, 2, 0),
       (21, 'Second on Board 2', 3, 2, 1);
```

### Ожидаемое состояние после V3

```yaml
cards:
  - id: 10, creator_id: 1, assignee_id: null  # board.author_id = 1
  - id: 11, creator_id: 1, assignee_id: null
  - id: 20, creator_id: 2, assignee_id: null  # board.author_id = 2
  - id: 21, creator_id: 2, assignee_id: null
```

---

## 8. Open Questions и Assumptions

### Assumptions (из story)

| ID | Assumption | Статус |
|----|------------|--------|
| A-3 | Board.author NOT NULL для всех записей | confirmed |
| A-2 | User entity и UserRepository существуют | confirmed |
| A-6 | Существующие тесты не сломаются | confirmed |

### Open Questions

| ID | Вопрос | Решение |
|----|--------|---------|
| OQ-5 | Создавать ли индексы сразу? | Да (решено — создавать в V3) |

---

## 9. Реализация тестов (рекомендации)

### Миграционный IT (MigrationIntegrationTest.java)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"/sql/seed-pre-v3.sql"})  // users, boards, columns, cards
class MigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void v3Migration_addsCreatorIdColumn() {
        // Given: V3 applied by Flyway on startup
        // Then: creator_id exists
        var columns = jdbc.queryForList(
            "SELECT column_name, is_nullable FROM information_schema.columns " +
            "WHERE table_name = 'cards' AND column_name = 'creator_id'");
        assertThat(columns).hasSize(1);
        assertThat(columns.get(0).get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void v3Migration_backfillCreatorIdEqualsBoardAuthor() {
        var mismatches = jdbc.queryForList(
            "SELECT c.id FROM cards c " +
            "JOIN boards b ON c.board_id = b.id " +
            "WHERE c.creator_id != b.author_id OR c.creator_id IS NULL");
        assertThat(mismatches).isEmpty();
    }

    @Test
    void v3Migration_addsForeignKeyOnCreatorId() {
        var fks = jdbc.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints " +
            "WHERE table_name = 'cards' AND constraint_type = 'FOREIGN KEY' " +
            "AND constraint_name = 'fk_cards_creator'");
        assertThat(fks).hasSize(1);
    }
}
```

### Рекомендуемый Java-класс

- `src/test/java/.../migration/V3MigrationIntegrationTest.java`
- Использовать `@Sql` для seed pre-V3 данных
- Проверять через `JdbcTemplate` (без entity — их ещё нет)

---

## 10. Заключение

ATDD-сценарии подготовлены для **Story 4.1 (V3 миграция)**. Всего **12 тестов** (6 P1, 4 P2, 2 P3), покрывающих:

- Чистое применение миграции — AC-1
- Backfill существующих карточек — AC-2 (R-07, P1-005)
- Ограничения и индексы — AC-3 (G-04)
- Регрессию существующих V1/V2
- Совместимость с H2 MODE=PostgreSQL
- Идемпотентность повторного применения

**Критические точки:**
1. Порядок операций в SQL: ADD → UPDATE → SET NOT NULL → CONSTRAINT → INDEX
2. Все карточки после backfill имеют `creator_id = board.author_id`
3. SET NOT NULL не падает — assumption A-3 валидна
4. FK и индексы созданы после backfill
