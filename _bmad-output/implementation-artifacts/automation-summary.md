---
workflowStatus: "completed"
node: "ai-automate-tests"
attempt: 2
primarySkill: "bmad-tea@1.0"
intent: "automate_atdd"
autoAdvance: true
inputDocuments:
  - "_bmad-output/test-artifacts/test-design-qa.md"
  - "_bmad-output/test-artifacts/test-design-architecture.md"
  - "_bmad-output/test-artifacts/atdd-scenarios-story-4.2.md"
  - "_bmad-output/planning-artifacts/stories/story-4.2-card-entity-dto.md"
knowledgeFragments:
  - "fixture-architecture"
  - "api-testing-patterns"
  - "selective-testing"
  - "contract-testing"
  - "feature-flags"
  - "test-quality"
  - "test-levels-framework"
  - "data-factories"
  - "ci-burn-in"
  - "risk-governance"
  - "probability-impact"
  - "test-priorities-matrix"
date: "2026-07-22"
---

# Automation Summary — Story 4.2: Card entity + DTO — поля creator/assignee и единый mapper

## 1. Обзор автоматизации

**Epic:** 4 — Параметры пользователей на карточках (creator + assignee)
**Story:** 4.2 — Card entity + DTO — поля creator/assignee и единый mapper
**Story Status:** ready-for-dev
**Baseline:** 19e5c2287906973db0532b1275e36fc95312697a

**Стек проекта:** Java 17+ / Spring Boot / JUnit 5 / MockMvc / H2 (MODE=PostgreSQL) / Flyway
**Уровни автоматизации:** API IT (MockMvc) — 100%; код-ревью (structural) — через ATDD-3.1/3.2
**FE автоматизация:** Не требуется (Story 4.2 — только backend Java)

**План автоматизации:**
- 14 ATDD-сценариев (9 × P0, 3 × P1, 2 × P2)
- Все — API IT через MockMvc (существующая инфраструктура KanbanIntegrationTest)
- 4 регрессионных теста (снятие @Disabled)
- 1 structural code-review проверка (ATDD-3.1, ATDD-3.2)

---

## 2. Traceability Coverage: AC → ATDD → Уровень автоматизации

| AC ID | ATDD сценарии | Уровень | Framework | Статус |
|-------|---------------|---------|-----------|--------|
| AC-1 | ATDD-1.1, ATDD-1.2, ATDD-1.3 | API IT | MockMvc + JUnit 5 | to-implement |
| AC-2 | ATDD-2.1, ATDD-2.2 | API IT | MockMvc + JUnit 5 | to-implement |
| AC-3 | ATDD-3.1, ATDD-3.2, ATDD-3.3 | Code review + API IT | Structural scan + MockMvc | to-implement |
| AC-4 | ATDD-4.1, ATDD-4.2, ATDD-4.3 | API IT | MockMvc + JUnit 5 | to-implement |
| AC-5 | ATDD-5.1 | Regression IT | MockMvc + JUnit 5 | to-enable |

**Покрытие:** 5/5 AC покрыты автоматизированными сценариями. Нет gaps.

---

## 3. API / Integration Test Automation (MockMvc)

### 3.1 Настройка тестовой инфраструктуры

**Существующая база:** `KanbanIntegrationTest` использует `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + Flyway для H2.

**Необходимые добавочные фикстуры (на основе data-factories + fixture-architecture):**

```java
// Bootstrap ADMIN (существует из V1/V2)
User admin = userRepository.findByUsername("admin").orElseThrow();

// Второй ADMIN для assignee тестов
User admin2 = seedUser("admin2", "password2");
```

**Helper-методы (pure function → fixture pattern):**

```java
// Seed helper — чистая функция + fixture
private User seedAdminUser(String username, String password) {
    User user = new User(username, passwordEncoder.encode(password), Role.ADMIN);
    return userRepository.save(user);
}

// Factory для Card с creator
private Card createCardWithCreator(String title, Long columnId, User creator) {
    Card card = new Card(title, columnId, creator); // новый конструктор с creator
    return cardRepository.save(card);
}
```

### 3.2 JSON-контракты (строгие vs нестрогие)

По рекомендации **api-testing-patterns**: для новых полей CardResponse используем строгую проверку (JsonPath.assertThat), для обратной совместимости — нестрогое сравнение (существующие тесты не должны сломаться).

**Пример строгой проверки (ATDD-1.1):**

```java
var json = JsonPath.parse(response.getResponse().getContentAsString());
assertThat(json.read("$.creatorId", Long.class)).isEqualTo(adminId);
assertThat(json.read("$.creatorUsername", String.class)).isEqualTo("admin");
assertThat(json.read("$.assigneeId", Long.class)).isNull();
assertThat(json.read("$.assigneeUsername", String.class)).isNull();
```

### 3.3 Обработка LAZY fetch в тестах

Поскольку `open-in-view=false` и используется `FetchType.LAZY`, все тесты должны вызывать getCreator().getUsername() и getAssignee() внутри `@Transactional` service-метода. В MockMvc тестах это гарантируется сервисным слоем. Прямые вызовы в тестах — через репозиторий в `@BeforeEach` / `@BeforeTransaction`.

---

## 4. Regression Automation

### 4.1 Снятие @Disabled (ATDD-5.1)

4 теста `KanbanIntegrationTest` разблокируются после имплементации Card.java:

| Тест | Причина @Disabled | Условие снятия |
|------|-------------------|----------------|
| cardCrudAndMoveNormalizeStablePositions | NOT NULL creator_id (V3) | Card entity предоставляет creator |
| invalidMoveDoesNotChangePersistedOrder | NOT NULL creator_id (V3) | Card entity предоставляет creator |
| deletingBoardCascadesColumnsAndCards | NOT NULL creator_id (V3) | Card entity предоставляет creator |
| resourcesOwnedByAnotherAdminAreHidden | NOT NULL creator_id (V3) | Card entity предоставляет creator |

**Действие:** удалить `@Disabled` с каждого теста. Никакой другой модификации тестов не требуется, так как bootstrap ADMIN автоматически используется как creator.

### 4.2 Обратная совместимость (ATDD-INT-2.1, ATDD-INT-2.2)

- Запросы createCard без assigneeId — 201 Created (assigneeId опционален)
- Нестрогое сравнение JSON — существующие тесты игнорируют новые поля
- assigneeId = null в createCard — равнозначно отсутствию (assignee = null)

---

## 5. Contract Testing Checks

### 5.1 CardResponse DTO contract

Story 4.2 вводит единый `CardResponse.from(Card)` — это первый шаг к формальному contract testing. На данном этапе:

- **Provider (backend):** CardResponse — единый источник истины для всех потребителей
- **Consumers:** BoardService, CardService — оба делегируют `CardResponse.from(Card)`
- **Contract verification:** ATDD-3.3 (INTEGRATION) проверяет, что оба сервиса возвращают идентичные CardResponse для одной карточки

**Рекомендация по будущему внедрению Pact:**
Когда появится frontend-потребитель (React), следует добавить Pact consumer test:

```typescript
// pact/consumer/card-response.pact.spec.ts
// Consumer: bmad-todolist-web
// Provider: bmad-todolist-api
// Endpoint: GET /api/cards/{id}
// Scrutiny: CardResponse содержит creatorId, creatorUsername, assigneeId, assigneeUsername
```

### 5.2 Provider Scrutiny (в контексте Story 4.2)

| # | Проверка | Источник | Результат |
|---|----------|----------|-----------|
| 1 | Response shape: CardResponse — плоский DTO | architecture-delta §4.2 | flat JSON, 12 полей |
| 2 | Status codes: 200 (GET/PUT), 201 (POST) | существующие контроллеры | без изменений |
| 3 | Field names: camelCase | Java conventions | creatorId, creatorUsername и т.д. |
| 4 | Required fields: creator NOT NULL | Card.java | creatorId/creatorUsername всегда присутствуют |
| 5 | Nullable: assigneeId/assigneeUsername | Card.java | null если нет assignee |

---

## 6. Feature Flag Checks

**Вывод:** Story 4.2 не требует feature flags. Изменения entity/DTO/mapper — инфраструктурные, применяются безусловно. Feature flag может понадобиться в Story 4.3 (service logic — assignee management), если потребуется постепенный rollout.

**Проверка на будущее (Story 4.3+):**
- enum `FLAGS.NEW_ASSIGNEE_LOGIC` — централизованное определение
- тесты обоих состояний (enabled/disabled)
- cleanup targeting после каждого spec

---

## 7. Rollback Validation Checks

### 7.1 Критерии rollback-безопасности

| Проверка | Действие | Ожидание |
|----------|----------|----------|
| R-01: Откат entity | revert Card.java до baseline | Компиляция успешна |
| R-02: Откат DTO | revert CardDtos.java до baseline | Компиляция успешна |
| R-03: Откат mapper | revert BoardService.java + CardService.java | toCardResponse — исходный конструктор |
| R-04: V3 migration stay | schema creator_id/assignee_id columns exist | Rollback Java кода не требует миграции |
| R-05: Тесты после rollback | вернуть @Disabled на 4 теста | KanbanIntegrationTest — PASS |
| R-06: CI pipeline | `mvn test` после rollback | BUILD SUCCESS |

**Важно:** V3 миграция (Story 4.1) не откатывается — она является prerequisite. Откат Java-кода Story 4.2 безопасен, так как schema уже готова и не меняется.

---

## 8. CI Integration & Selective Execution

### 8.1 Предлагаемый pipeline (основываясь на CI strategy)

| Stage | Команда | Таймаут | Gate |
|-------|---------|---------|------|
| **pre-commit** | `mvn compile` | 2 мин | Блокирующий |
| **pre-commit** | `mvn test -pl backend -Dtest=KanbanIntegrationTest` | 5 мин | Блокирующий |
| **CI PR** | `mvn test` (полный suite) | 10 мин | Блокирующий |
| **CI merge** | `mvn test` + `npm run lint && npm run build` (FE) | 15 мин | Блокирующий |

### 8.2 Selective execution по тегам

Сценарии ATDD группируются по risk priority:

```bash
# P0 только (критические — fast feedback)
mvn test -Dtest=KanbanIntegrationTest -Dgroups="P0"

# P0 + P1 (полный набор для PR)
mvn test -Dtest=KanbanIntegrationTest -Dgroups="P0,P1"

# Полная регрессия (pre-merge)
mvn test
```

**Замечание:** JUnit 5 Tag Expressions (`@Tag("P0")`) — существующий механизм; все новые тесты должны быть помечены тегом приоритета.

### 8.3 Параметры sharding (для больших наборов)

```yaml
# GitHub Actions matrix
strategy:
  fail-fast: false
  matrix:
    shard: [1, 2, 3]
steps:
  - run: mvn test -Dtest=KanbanIntegrationTest -Dshard=${{ matrix.shard }}/3
```

---

## 9. План имплементации тестов

### Фаза 1: Инфраструктура (до реализации кода)

- [ ] Добавить seedUser helper в KanbanIntegrationTest или BaseIntegrationTest
- [ ] Проверить, что 4 @Disabled теста действительно падают без изменений entity (подтверждение baseline)
- [ ] Создать тестовые JSON шаблоны (request body templates)

### Фаза 2: ATDD-реализация (после реализации кода)

- [ ] ATDD-1.1: createCard без assignee — creator=current user, assignee=null
- [ ] ATDD-1.2: createCard с assignee — creator+assignee заполнены
- [ ] ATDD-1.3: createCard с явным null assigneeId
- [ ] ATDD-2.1: Board detail — карточки содержат 4 поля
- [ ] ATDD-2.2: GET /api/boards summary не содержит карточек
- [ ] ATDD-3.1/3.2: code review — проверить отсутствие new CardResponse(...) в сервисах
- [ ] ATDD-3.3: BoardService.getBoard vs CardService.getCard — идентичные CardResponse
- [ ] ATDD-4.1: updateCard resetAssignee=true — сброс assignee
- [ ] ATDD-4.2: updateCard без assigneeId — assignee не меняется
- [ ] ATDD-4.3: updateCard assigneeId=null + resetAssignee=false
- [ ] ATDD-5.1: снять @Disabled — все 4 регрессионных теста проходят
- [ ] ATDD-INT-1.1: createCard без сессии — 401
- [ ] ATDD-INT-1.2: GET /api/boards/{id} — LazyInitializationException не возникает
- [ ] ATDD-INT-2.1: старый запрос без assigneeId — 201
- [ ] ATDD-INT-2.2: нестрогое сравнение — существующие тесты не ломаются

### Фаза 3: Валидация

- [ ] `mvn test` — 0 failures, 0 errors
- [ ] Проверка отчёта Allure/SPR (если интегрирован)
- [ ] Проверка лога сборки FE (если применимо)

---

## 10. Quality Gates & KPI

### Gate: pre-commit локально

| Gate | Порог | Метрика |
|------|-------|---------|
| G-01 (CardResponse.from) | PASS | Code review: BoardService + CardService делегируют |
| G-02 (resetAssignee) | PASS | ATDD-4.1, ATDD-4.2, ATDD-4.3 — все PASS |
| C-01 | BUILD SUCCESS | mvn compile |
| C-03 | 0 failures | Все P0 ATDD (MockMvc) |

### Gate: CI PR

| Gate | Порог | Метрика |
|------|-------|---------|
| C-04 | 100% PASS | KanbanIntegrationTest |
| P0-006 (TS-06) | PASS | Board detail cards содержат 4 поля |
| C-07 | PASS | resetAssignee семантика |

### Метрики качества (из test-quality)

| Метрика | Цель | Текущее |
|---------|------|---------|
| Время выполнения ATDD | < 2 мин | ~20-40 секунд (MockMvc) |
| Размер теста | < 300 строк | каждый тест < 50 строк |
| Deterministic | нет hard waits | MockMvc — синхронный, детерминированный |
| Isolation | self-cleaning | каждый тест — изолированный контекст |
| Flakiness rate | 0% | MockMvc тесты стабильны |

---

## 11. Покрытие рисков

| Risk ID | Score | Покрытие | Статус |
|---------|-------|----------|--------|
| R-01 | 9 (CRITICAL) | ATDD-1.1, ATDD-1.2, ATDD-1.3, ATDD-2.1, ATDD-2.2, ATDD-3.1, ATDD-3.2, ATDD-3.3 | ✅ полное |
| R-03 | 4 (MEDIUM) | ATDD-4.1, ATDD-4.2, ATDD-4.3 | ✅ полное |
| R-04 | 4 (MEDIUM) | ATDD-INT-2.1, ATDD-INT-2.2 | ✅ полное |
| R-05 | 6 (HIGH) | ATDD-INT-1.1 (401 без сессии) | ✅ частичное (Story 4.3 — валидация assigneeId) |
| R-08 | 1 (LOW) | — (moveCard не меняет assignee) | ✅ не требуется |
| R-10 | 3 (LOW) | ATDD-5.1 (регрессия) | ✅ полное |

**Остаточный риск:** R-05 (валидация assigneeId) будет полностью покрыт в Story 4.3 (service logic). На уровне Story 4.2 достаточно проверки, что createCard без assignee работает (R-04) и без сессии отклоняется (ATDD-INT-1.1).

---

## 12. Оценка трудоёмкости автоматизации

| Фаза | Задачи | Оценка (человеко-часы) |
|------|--------|------------------------|
| Phase 1: Инфраструктура | seed helper, fixture setup | 1 ч |
| Phase 2: ATDD (14 сценариев) | API IT тесты | 4-6 ч |
| Phase 3: Regression enable | снятие @Disabled, верификация | 0.5 ч |
| Phase 4: Code review | structural проверка ATDD-3.1/3.2 | 0.5 ч |
| Phase 5: CI integration | pipeline configuration | 1 ч |
| **Итого** | | **7-9 ч** |

---

## 13. Изменения в проекте

### KanbanIntegrationTest.java (MODIFY)

```java
// Добавить seed helper:
private User seedAdminUser(String username, String password) {
    User user = new User(username, passwordEncoder.encode(password), Role.ADMIN);
    return userRepository.save(user);
}

// НОВЫЕ ТЕСТЫ:
// ATDD-1.1 — createCard без assignee
// ATDD-1.2 — createCard с assignee
// ATDD-1.3 — createCard с null assigneeId
// ATDD-2.1 — Board detail содержит 4 поля
// ATDD-2.2 — Board summary не содержит карточек
// ATDD-3.3 — CardResponse идентичен в BoardService и CardService
// ATDD-4.1 — resetAssignee=true
// ATDD-4.2 — updateCard без assigneeId
// ATDD-4.3 — assigneeId=null + resetAssignee=false
// ATDD-INT-1.1 — 401 без сессии
// ATDD-INT-1.2 — LazyInitializationException check
// ATDD-INT-2.1 — обратная совместимость запроса
// ATDD-INT-2.2 — нестрогое сравнение JSON

// MODIFY — снять @Disabled с 4 тестов:
// - cardCrudAndMoveNormalizeStablePositions
// - invalidMoveDoesNotChangePersistedOrder
// - deletingBoardCascadesColumnsAndCards
// - resourcesOwnedByAnotherAdminAreHidden
```

---

## 14. Риски автоматизации

| Риск | Описание | Mitigation |
|------|----------|------------|
| ATDD-3.1/3.2 структурные — не могут быть полностью автоматизированы в CI | Поиск `new CardResponse(...)` в сервисах — code review | Добавить статический анализатор (ArchTest из ArchUnit) для будущих спринтов |
| ATDD-2.2 (Board summary не содержит карточек) — может неявно измениться | Новый mapper может повлиять на структуру BoardResponse | Явный assert на размер/отсутствие `cards` |
| ATDD-4.3 (null assigneeId + resetAssignee=false) — Jackson поведение | Jackson может не различать null vs absent при определённых настройках | Использовать `@JsonInclude(NON_NULL)` или `Optional<Long>` для assigneeId |

---

## 15. Заключение

**Все 14 ATDD-сценариев** для Story 4.2 подлежат автоматизации через существующий MockMvc/JUnit 5 стек. Новая инфраструктура минимальна (seed второго ADMIN). 4 регрессионных теста разблокируются после имплементации entity.

**Рекомендуемый порядок реализации тестов:**
1. Дождаться реализации Card.java + CardDtos.java + mapper
2. Включить seed helper для admin2
3. Реализовать ATDD-1.x → ATDD-2.x → ATDD-4.x → ATDD-INT-x.x
4. Снять @Disabled (ATDD-5.1)
5. Провести code review structural checks (ATDD-3.x)
6. Запустить полный suite — валидация

**Generated by:** Murat (Master Test Architect) — TEA headless flow
**Workflow:** bmad-tea@1.0 automate_atdd (via bmad-testarch-automate knowledge patterns)
**HGSDLC Node:** ai-automate-tests (attempt-2)
**Date:** 2026-07-22
