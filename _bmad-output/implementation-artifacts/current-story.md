---
id: "4.1"
epic: 4
story: 1
key: "4-1-v3-миграция-добавление-creator-id-и-assignee-id-в-таблицу-cards"
title: "V3 миграция — добавление creator_id и assignee_id в таблицу cards"
status: "ready-for-dev"
created: "2026-07-22"
updated: "2026-07-22"
feature: "Параметры пользователей на карточках (creator + assignee)"
---

# Story 4.1: V3 миграция — добавление creator_id и assignee_id в таблицу cards

## Цель

Добавить поля `creator_id` (NOT NULL) и `assignee_id` (nullable) в таблицу `cards` через новую Flyway миграцию с backfill существующих записей, чтобы entity и сервисы могли опираться на готовую схему.

## Affected service

`backend/` — только база данных (Flyway V3 миграция)

## Scope

- Создание `V3__add_card_users.sql` — ALTER TABLE cards, ADD COLUMN, backfill, SET NOT NULL, ADD CONSTRAINT, CREATE INDEX
- Никаких изменений Java-кода, entity, DTO, сервисов или контроллеров
- Никаких изменений фронтенда

## Acceptance Criteria

### AC-1 (P0): Чистая миграция
**Given** чистая БД без полей creator_id/assignee_id  
**When** применяется Flyway V3 (`V3__add_card_users.sql`)  
**Then** в таблице `cards` появляются колонки:
- `creator_id` BIGINT NOT NULL
- `assignee_id` BIGINT nullable

### AC-2 (P1-005, P0-006 → R-07): Backfill существующих карточек
**Given** в БД есть карточки, созданные до V3  
**When** миграция выполнена  
**Then** все существующие карточки получают `creator_id` = `board.author_id` (AC-7, P1-005)

### AC-3 (G-04): Ограничения и индексы
**Given** миграция применена  
**When** проверяется схема  
**Then** 
- `creator_id` имеет FK → `users(id)`, `assignee_id` имеет FK → `users(id)`
- существуют индексы на `creator_id` и `assignee_id`

## Implementation Notes

### V3 миграция (REQ-1)

```sql
-- V3__add_card_users.sql
ALTER TABLE cards
    ADD COLUMN creator_id BIGINT,
    ADD COLUMN assignee_id BIGINT;

UPDATE cards c
SET creator_id = b.author_id
FROM boards b
WHERE c.board_id = b.id;

ALTER TABLE cards
    ALTER COLUMN creator_id SET NOT NULL,
    ADD CONSTRAINT fk_cards_creator FOREIGN KEY (creator_id) REFERENCES users(id);

ALTER TABLE cards
    ADD CONSTRAINT fk_cards_assignee FOREIGN KEY (assignee_id) REFERENCES users(id);

CREATE INDEX idx_cards_creator_id ON cards(creator_id);
CREATE INDEX idx_cards_assignee_id ON cards(assignee_id);
```

### Critical implementation requirements

1. **Порядок операций строгий**: ADD COLUMN (nullable) → UPDATE backfill → SET NOT NULL → ADD CONSTRAINT → CREATE INDEX. Нарушение порядка даст ошибку NOT NULL violation.
2. **Backfill через JOIN**: `UPDATE cards SET creator_id = board.author_id FROM boards WHERE cards.board_id = board.id`. Все существующие карточки принадлежат board с author_id (A-3).
3. **НЕ трогать существующие V1/V2** — создаётся только новый файл `V3__add_card_users.sql`.
4. **Два FK**: `creator_id → users(id)` и `assignee_id → users(id)`.
5. **Два индекса**: на `creator_id` и `assignee_id` (решение OQ-5 — создать сразу).
6. **`ddl-auto=validate`** — не менять; Flyway управляет схемой.
7. **Профиль `test` (H2 `MODE=PostgreSQL`)** — проверить, что SQL совместим. H2 `ALTER TABLE ... ALTER COLUMN SET NOT NULL` работает; `FROM` в UPDATE поддерживается в H2 mode PostgreSQL.

### Business rules applied

- BR-6: `creator_id` = Board.author_id для существующих карточек (backfill)
- BR-8: `creator_id` NOT NULL после backfill
- `assignee_id` nullable
- FK на `users(id)` для обоих полей

### Validation report findings applied

- OQ-5 (решено: создавать индексы сразу)
- G-04: V3 backfill creator_id = Board.author; NOT NULL после проверки

## Dependencies

| Зависимость | Тип | Статус |
|---|---|---|
| Board.author NOT NULL (A-3) | assumption | confirmed |
| User entity существует (V1/V2) | assumption | confirmed |
| Существующие V1/V2 миграции | hard dependency | done |
| H2 MODE=PostgreSQL совместимость | verification needed | проверить перед PR |

## Test Notes

### P0/P1 тесты (из TEA handoff)

| Test ID | Описание | Risk |
|---|---|---|
| P1-005 (TS-11) | V3 миграция: backfill creator_id = Board.author | R-07 (Score 6) |
| IT-10 | Migration IT: creator_id = Board.author для существующих карточек | R-07 |

### Testability concerns (из test-design-architecture.md)

- **R-07 (Score 6) — HIGH**: V3 миграция — backfill creator_id может дать NULL на повреждённых данных.
  - Mitigation: SET NOT NULL после проверки; тестовая миграция на копии данных.
  - Verification: IT после V3: у всех карточек creator_id IS NOT NULL и равен Board.author_id.

### Test approach

- Migration IT: применить V3 на H2 с seed-данными (существующие карточки), проверить backfill + NOT NULL + FK + индексы
- Интеграция в существующий `@SpringBootTest` с `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- Новый файл: `MigrationIntegrationTest.java` или расширить `KanbanIntegrationTest`

### Risk priorities

| Risk ID | Score | Category | Mitigation |
|---|---|---|---|
| R-07 | 6 | DATA | V3 backfill + SET NOT NULL после валидации; тестовая миграция |

## Rollback Notes

- **Backend**: откатить V3 через `Flyway undo` (если настроено) или через `V3.1__revert_add_card_users.sql`:
  - DROP INDEX idx_cards_assignee_id, idx_cards_creator_id
  - ALTER TABLE cards DROP CONSTRAINT fk_cards_assignee, fk_cards_creator
  - ALTER TABLE cards DROP COLUMN assignee_id, creator_id
- **Rollback безопасен**: assignee_id nullable позволяет просто игнорировать; creator_id NOT NULL не снимается без даунтайма.
- **Frontend**: откат FE до предыдущей версии (если FE уже обновлён).

## Architecture Delta References

- **Архитектурная дельта**: `_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/architecture-delta.md`
  - §4.5 Flyway V3 — миграция (строка 204–225)
  - §3.3 Data/DB/ownership
  - §5.3 Contract/migration tests (IT-10)
  - §6.2 Migration/backfill/compatibility
  - §6.4 Rollback

## Test Design References

- **test-design-architecture.md**: §R-07, §Mitigation Plans (R-07), §Testability Concerns
- **test-design-qa.md**: P1-005 (TS-11), §Risk Assessment (R-07)
- **TEA Handoff**: G-04, R-07, P1-005

## Open Questions

Отсутствуют. Все OQ из addendum §9 решены в architecture delta. Новых вопросов по миграции не выявлено.

## Assumptions

| ID | Assumption | Обоснование |
|---|---|---|
| A-2 | User entity и UserRepository уже существуют (V1/V2) | PRD addendum §10; project-context |
| A-3 | Board.author NOT NULL; используется для backfill | PRD addendum §10; existing model |
| A-6 | Существующие тесты не сломаются от новых полей (нестрогое сравнение) | PRD addendum §10 |
