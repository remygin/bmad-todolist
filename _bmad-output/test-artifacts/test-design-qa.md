---
workflowStatus: 'completed'
totalSteps: 5
stepsCompleted: [1, 2, 3, 4, 5]
lastStep: '5'
nextStep: ''
lastSaved: '2026-07-22'
workflowType: 'testarch-test-design'
inputDocuments:
  - '_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md'
  - '_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md'
  - '_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/architecture-delta.md'
  - '_bmad-output/test-artifacts/test-design-architecture.md'
---

# Test Design for QA: Параметры пользователей на карточках (creator + assignee)

**Purpose:** Test execution recipe для QA. Определяет что тестировать, как тестировать и что нужно от других команд.

**Date:** 2026-07-22
**Author:** Murat (Master Test Architect)
**Status:** Draft
**Project:** bmad-todolist

**Related:** Architecture doc (`test-design-architecture.md`) — testability concerns and blockers.

---

## Executive Summary

**Scope:** API-тестирование creator + assignee на карточках: createCard, updateCard, Board detail, GET /api/users, V3 migration. FE проверка — ручная через сборку (lint + build, без Playwright).

**Risk Summary:**
- Всего рисков: 8 (3 high-priority ≥6, 2 medium, 3 low)
- Critical Categories: Технический (TECH) — рассинхронизация mapper; Целостность данных (DATA) — валидация assigneeId + V3 миграция

**Coverage Summary:**
- P0 тесты: 6 (критические paths, security, data integrity)
- P1 тесты: 8 (важные фичи, интеграция)
- P2 тесты: 6 (edge cases, regression)
- P3 тесты: 4 (exploratory)
- **Всего:** ~24 теста (~1.5 недели для 1 QA)

---

## Not in Scope

| Item | Reasoning | Mitigation |
|---|---|---|
| **FE E2E (Playwright)** | В проекте нет Playwright infra; gate — `npm run lint` + `npm run build` | Ручная проверка отображения creator/assignee; API тесты покрывают логику |
| **UI-компоненты assignee select** | CardForm assignee dropdown — отображение данных; логика на сервере | API тесты на GET /api/users; FE build проверяет типы |
| **Производительность** | Single-user, нагрузка минимальна | Smoke performance тест при необходимости |
| **Смена creator** | Инвариант (BR-8) — creator не меняется после создания | Единый тест на попытку смены → игнорирование |

---

## Dependencies & Test Blockers

### Backend/Architecture Dependencies (Pre-Implementation)

1. **Единый CardResponse mapper (B-01)** — Backend — pre-implementation
   - Без него Board.getBoard не возвращает creator/assignee
   - Блокирует P0-006 (TS-06)

2. **Jackson deserialization assigneeId (B-02)** — Backend — pre-implementation
   - UpdateCardRequest требует различения absent vs null
   - Блокирует P1-006 (TS-13)

### QA Infrastructure Setup (Pre-Implementation)

1. **Тестовые фикстуры User** — QA
   - Для тестов assignee нужен второй пользователь с ролью ADMIN
   - Можно добавить через SQL `INSERT INTO users` в `@Sql` или через UserRepository в `@BeforeEach`
   - Backfill creator_id тест использует существующего bootstrap ADMIN

2. **Test Environments** — QA
   - Local: MockMvc + H2 (MODE=PostgreSQL) — существующий профиль `test`
   - CI/CD: `mvn test` — существующий pipeline

---

## Risk Assessment

### High-Priority Risks (Score ≥6)

| Risk ID | Category | Description | Score | QA Test Coverage |
|---|---|---|---|---|
| **R-01** | TECH | Рассинхронизация BoardService.toCardResponse | **9** | P0-006: Board detail cards содержат 4 поля |
| **R-05** | DATA | Валидация assigneeId | **6** | P0-003, P1-001, P1-002: 400 на невалидный assigneeId |
| **R-07** | DATA | V3 миграция backfill + NOT NULL | **6** | P1-005: backfill creator_id для существующих карточек |

### Medium/Low-Priority Risks

| Risk ID | Category | Description | Score | QA Test Coverage |
|---|---|---|---|---|
| R-03 | TECH | Jackson null vs absent assigneeId | 4 | P1-006: updateCard без assigneeId → assignee не меняется |
| R-04 | BUS | Обратная совместимость API | 4 | P2-001: старые запросы без assigneeId работают |
| R-06 | SEC | GET /api.users data exposure | 3 | P1-003, P1-004: 401/403 тесты |
| R-10 | TECH | Инвариант трёх колонок | 3 | Регрессия в существующих тестах kanban |
| R-08 | OPS | FE optimistic update | 1 | Не требуется (move не трогает assignee) |

---

## NFR Test Coverage Plan

| NFR Category | Requirement / Threshold | Planned Validation | Tool / Level | Evidence Artifact | Priority |
|---|---|---|---|---|---|
| Security | GET /api/users — только ADMIN | 401 без токена; 403 без ADMIN | API IT | test report | P1 |
| Security | assigneeId валидация: существование + ADMIN | 400 на невалидный assigneeId | API IT | test report | P0 |
| Data Integrity | creator NOT NULL, backfill корректен | V3 миграция проверена | Migration IT | test report | P1 |
| Data Integrity | CardResponse единый источник | Board detail cards содержат 4 поля | API IT | test report | P0 |
| Maintainability | FE типы синхронизированы | npm run lint + build | CI gate | CI output | P2 |

**Missing thresholds or evidence sources:** Не выявлены.

---

## Entry Criteria

- [x] PRD + addendum согласованы (status: draft)
- [x] Architecture delta утверждена (decision в §8 Spine ADR updates)
- [ ] Pre-implementation blockers (B-01, B-02) resolved
- [ ] V3 миграция написана и проверена на H2
- [ ] Feature deployed to test (MockMvc tests runnable)

## Exit Criteria

- [ ] Все P0 тесты проходят
- [ ] Все P1 тесты проходят (или failures triaged и accepted)
- [ ] Нет open high-priority / high-severity багов
- [ ] V3 миграция проверена: корректен backfill creator_id
- [ ] FE build + lint проходят

---

## Test Coverage Plan

### P0 (Critical)

**Criteria:** Security-critical + High risk (≥6) + Data integrity + Core functionality

| Test ID | Requirement | Test Level | Risk Link | Notes |
|---|---|---|---|---|
| **P0-001 (TS-01)** | FR-11: createCard без assignee — creator=current user, assignee=null | API IT (MockMvc) | R-05 | AC-1 |
| **P0-002 (TS-02)** | FR-11: createCard с валидным assigneeId — creator+assignee заполнены | API IT (MockMvc) | R-05 | AC-2 |
| **P0-003 (TS-03)** | FR-11: createCard с несуществующим assigneeId → 400 | API IT (MockMvc) | R-05 | AC-3 |
| **P0-004 (TS-04)** | FR-12: updateCard — смена assignee | API IT (MockMvc) | R-05 | AC-5 |
| **P0-005 (TS-05)** | FR-12: updateCard — сброс assignee (resetAssignee=true) | API IT (MockMvc) | R-05 | AC-6 |
| **P0-006 (TS-06)** | FR-5: Board detail — карточки содержат creator/assignee поля | API IT (MockMvc) | **R-01** | RR-1 coverage |

**Total P0:** 6 tests

---

### P1 (High)

**Criteria:** Important workflows + Medium risk + Edge cases

| Test ID | Requirement | Test Level | Risk Link | Notes |
|---|---|---|---|---|
| **P1-001 (TS-07)** | FR-11: createCard c assigneeId=0 → 400 | API IT | R-05 | EC-1 |
| **P1-002 (TS-08)** | FR-12: updateCard c несуществующим assigneeId → 400 | API IT | R-05 | BR-3 |
| **P1-003 (TS-09)** | OQ-2: GET /api/users — 200, возвращает список ADMIN | API IT | R-06 | UserIntegrationTest |
| **P1-004 (TS-10)** | OQ-2: GET /api/users — 401 без токена | API IT | R-06 | Security |
| **P1-005 (TS-11)** | AC-7: V3 миграция — backfill creator_id = Board.author | Migration IT | **R-07** | Flyway test |
| **P1-006 (TS-12)** | FR-12: updateCard — title + assigneeId одновременно | API IT | R-05 | EC-3 |
| **P1-007 (TS-13)** | FR-12: updateCard без assigneeId — assignee не меняется | API IT | R-03 | null vs absent |
| **P1-008 (TS-14)** | FR-14: moveCard — assignee не меняется | API IT | R-08 | EC-6 |

**Total P1:** 8 tests

---

### P2 (Medium)

**Criteria:** Secondary features + Low risk + Edge cases + Regression prevention

| Test ID | Requirement | Test Level | Risk Link | Notes |
|---|---|---|---|---|
| **P2-001 (TS-15)** | EC-2: creator = assignee (assigneeId = current user) | API IT | — | Valid scenario |
| **P2-002 (TS-16)** | EC-7: multiple cards with same assignee | API IT | — | No unique constraint |
| **P2-003 (TS-17)** | OQ-2: GET /api/users — 403 для non-ADMIN | API IT | R-06 | Spring Security test |
| **P2-004 (TS-18)** | FE: KanbanCard отображает creatorUsername | Manual (build) | — | Проверить после деплоя |
| **P2-005 (TS-19)** | FE: KanbanCard отображает assigneeUsername | Manual (build) | — | Проверить после деплоя |
| **P2-006 (TS-20)** | FE: CardForm загружает users из GET /api/users | Manual (build) | — | Проверить после деплоя |

**Total P2:** 6 tests

---

### P3 (Low)

**Criteria:** Nice-to-have + Exploratory

| Test ID | Requirement | Test Level | Notes |
|---|---|---|---|
| **P3-001 (TS-21)** | BR-8: creator не изменяется после создания | API IT | Попытка обновления creator игнорируется |
| **P3-002 (TS-22)** | UI тексты на русском | Manual | Поле creator «создал:», assignee «исполнитель:» |
| **P3-003 (TS-23)** | Cross-board assignee (ADMIN с другой доски) | API IT | EC-4: допустимо |
| **P3-004 (TS-24)** | assigneeId не-ADMIN → 400 | API IT | AC-4 |

**Total P3:** 4 tests

---

## Execution Strategy

### Every PR: MockMvc API Tests (~5-8 min)

**Все функциональные тесты (P0, P1, P2 API):**

- Все API IT через MockMvc + H2
- Используется существующий профиль `test` + Flyway
- Total: ~18 MockMvc тестов (P0: 6, P1: 8, P2: 4)

### Manual/FE (before merge)

- FE сборка: `npm run lint && npm run build`
- Визуальная проверка KanbanCard (creatorUsername, assigneeUsername)
- Визуальная проверка CardForm (assignee dropdown)

### Nightly

Не требуется — single-user, нагрузка минимальна.

---

## QA Effort Estimate

| Priority | Count | Effort Range | Notes |
|---|---|---|---|
| P0 | 6 | ~3-4 days | API IT: create/update/move с assignee + Board detail |
| P1 | 8 | ~3-4 days | Edge cases, Migration IT, GET /api/users |
| P2 | 6 | ~2-3 days | FE manual, дополнительные API edge cases |
| P3 | 4 | ~1 day | Exploratory, cross-board, creator invariant |
| **Total** | 24 | **~1.5 weeks** | **1 QA engineer, full-time** |

**Assumptions:**
- Включает test design, implementation, debugging, CI integration
- Исключает ongoing maintenance (~10% effort)
- Test infrastructure (factories) готова

---

## Interworking & Regression

| Service/Component | Impact | Regression Scope | Validation Steps |
|---|---|---|---|
| **CardController/Service** | +assigneeId в create/update; +creator/assignee в response | Существующие CRUD тесты | Прогнать существующие KanbanIntegrationTest |
| **BoardService** | toCardResponse делегирует единому mapper | getBoard, getBoardSummary | Проверить, что Board detail содержит новые поля |
| **UserController (new)** | GET /api/users | Нет — новый эндпоинт | New UserIntegrationTest |
| **Frontend api/kanban.ts** | +4 поля в Card; +assigneeId в create/update | npm run lint + build | Сборка без ошибок типов |

**Regression test strategy:**
- Прогнать существующие KanbanIntegrationTest — они не должны сломаться (нестрогое сравнение JSON, assigneeId опционален)
- Проверить обратную совместимость: старые запросы createCard без assigneeId работают

---

## Appendix A: Code Examples & Tagging

```java
// Пример структуры IT для KanbanIntegrationTest

@Test
void createCard_withoutAssignee_setsCreatorAndNullAssignee() throws Exception {
    // Given: аутентифицированный ADMIN + существующая доска
    var board = createBoard("Test Board");
    var todoColumn = getFirstColumn(board, ColumnStatus.TODO);

    // When: создание карточки без assigneeId
    var request = """
        {
            "title": "Test Card",
            "columnId": %d
        }
        """.formatted(todoColumn.id());

    var response = mockMvc.perform(post("/api/cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + adminToken)
            .content(request))
        .andExpect(status().isCreated())
        .andReturn();

    // Then: creator = current user, assignee = null
    var json = JsonPath.parse(response.getResponse().getContentAsString());
    assertThat(json.read("$.creatorId", Long.class)).isEqualTo(adminId);
    assertThat(json.read("$.creatorUsername", String.class)).isEqualTo("admin");
    assertThat(json.read("$.assigneeId", Long.class)).isNull();
    assertThat(json.read("$.assigneeUsername", String.class)).isNull();
}

@Test
void createCard_withValidAssignee_setsCreatorAndAssignee() throws Exception {
    // Given: seed второго ADMIN
    var secondUserId = seedAdminUser("admin2", "password2");

    // When: создание с assigneeId = secondUserId
    var request = """
        {
            "title": "Assigned Card",
            "columnId": %d,
            "assigneeId": %d
        }
        """.formatted(todoColumn.id(), secondUserId);

    // Then: creator + assignee заполнены
    var json = JsonPath.parse(mockMvc.perform(post("/api/cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + adminToken)
            .content(request))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString());

    assertThat(json.read("$.creatorId", Long.class)).isEqualTo(adminId);
    assertThat(json.read("$.assigneeId", Long.class)).isEqualTo(secondUserId);
}
```

---

## Appendix B: Knowledge Base References

- **Risk Governance**: `risk-governance.md` — Risk scoring methodology
- **Test Priorities Matrix**: `test-priorities-matrix.md` — P0-P3 criteria
- **Probability and Impact Scale**: `probability-impact.md` — Risk scoring definitions
- **ADR Quality Readiness Checklist**: `adr-quality-readiness-checklist.md` — NFR assessment criteria

---

**Generated by:** BMad TEA Agent (Murat)
**Workflow:** `bmad-testarch-test-design`
**Version:** 1.0
