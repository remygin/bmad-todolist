# ATDD Scenarios — Story 4.2: Card entity + DTO — поля creator/assignee и единый mapper

**Epic:** 4 — Параметры пользователей на карточках (creator + assignee)  
**Story:** 4.2 — Card entity + DTO — поля creator/assignee и единый mapper  
**Status:** ready-for-dev  
**Author:** Murat (Master Test Architect) — TEA headless flow  
**Date:** 2026-07-22  
**Method:** risk-based ATDD (TEA principles + bmad-testarch-atdd)

---

## 1. Coverage Traceability Matrix

| AC ID | Описание | Приоритет | Risk ID | Risk Score | Сценарий(и) ATDD | Уровень теста |
|-------|----------|-----------|---------|------------|------------------|---------------|
| **AC-1** | CardResponse содержит 4 поля creator/assignee | P0 | R-01 | 9 | ATDD-1.1, ATDD-1.2, ATDD-1.3 | API IT (MockMvc) |
| **AC-2** | Board detail — карточки с creator/assignee | P0 | R-01 | 9 | ATDD-2.1, ATDD-2.2 | API IT (MockMvc) |
| **AC-3** | Единый mapper CardResponse.from(Card) | P0 | R-01 | 9 | ATDD-3.1, ATDD-3.2 | Code review + API IT |
| **AC-4** | UpdateCardRequest — resetAssignee семантика | P1 | R-03 | 4 | ATDD-4.1, ATDD-4.2, ATDD-4.3 | API IT (MockMvc) |
| **AC-5** | Существующие тесты проходят | P0 | — | — | ATDD-5.1 | Regression IT |

---

## 2. Acceptance Scenarios

### 2.1 AC-1: CardResponse содержит 4 поля creator/assignee

**Уровень:** API IT (MockMvc)  
**Риск:** R-01 (Score 9 — CRITICAL)  
**Связь:** P0-006 (TS-06), FR-5

#### ATDD-1.1 (POSITIVE): createCard возвращает creatorId + creatorUsername (NOT NULL)

```gherkin
Given: аутентифицированный ADMIN с существующей доской и колонкой TODO
  And: Card entity расширена полями creator (ManyToOne, NOT NULL) и assignee (ManyToOne, nullable)
When: выполнен POST /api/cards с телом {"title": "Test Card", "columnId": <columnId>}
Then: статус 201 Created
  And: JSON-ответ содержит поля:
    | Поле            | Ожидание                          |
    | creatorId       | id текущего ADMIN (Long, NOT NULL) |
    | creatorUsername | "admin" (String, NOT NULL)         |
    | assigneeId      | null                               |
    | assigneeUsername| null                               |
```

**Критерий прохождения:** Все 4 поля присутствуют; creatorId совпадает с ID сессионного пользователя; creatorUsername совпадает с username сессионного пользователя; assigneeId == null; assigneeUsername == null.

#### ATDD-1.2 (POSITIVE): createCard с assigneeId заполняет creator + assignee

```gherkin
Given: аутентифицированный ADMIN
  And: seed второго ADMIN через UserRepository (admin2)
  And: существующая доска + колонка TODO
When: выполнен POST /api/cards с телом
  {"title": "Assigned Card", "columnId": <columnId>, "assigneeId": <secondUserId>}
Then: статус 201 Created
  And: creatorId == id текущего ADMIN
  And: creatorUsername == username текущего ADMIN
  And: assigneeId == secondUserId
  And: assigneeUsername == "admin2"
```

**Критерий прохождения:** 4 поля заполнены корректно; assignee отражает переданный ID.

#### ATDD-1.3 (EDGE): createCard с assigneeId = null (явный null)

```gherkin
Given: аутентифицированный ADMIN с существующей доской
When: выполнен POST /api/cards с телом
  {"title": "Null Assignee", "columnId": <columnId>, "assigneeId": null}
Then: статус 201 Created
  And: creatorId == id текущего ADMIN (NOT NULL)
  And: assigneeId == null
  And: assigneeUsername == null
```

**Критерий прохождения:** Явный null assigneeId трактуется как отсутствие назначения.

---

### 2.2 AC-2: Board detail — карточки с creator/assignee

**Уровень:** API IT (MockMvc)  
**Риск:** R-01 (Score 9 — CRITICAL)  
**Связь:** P0-006 (TS-06), IT-11

#### ATDD-2.1 (POSITIVE): BoardResponse содержит карточки с 4 полями

```gherkin
Given: доска с 2 карточками (одна с assignee, другая без)
  And: CardResponse.from(Card) — единый mapper реализован
When: выполнен GET /api/boards/{boardId}
Then: статус 200 OK
  And: для каждой карточки в JSON-ответе присутствуют поля:
    creatorId (NOT NULL Long)
    creatorUsername (NOT NULL String)
    assigneeId (Long или null)
    assigneeUsername (String или null)
```

**Критерий прохождения:** Все карточки в BoardResponse содержат 4 поля; assigneeId/assigneeUsername у карточки без assignee == null.

#### ATDD-2.2 (POSITIVE): GET /api/boards — summary-level не содержит карточек

```gherkin
Given: доска с карточками
When: выполнен GET /api/boards (список досок, summary)
Then: статус 200 OK
  And: ответ не содержит карточек (только мета-информация доски)
```

**Критерий прохождения:** Список досок не изменил контракт.

---

### 2.3 AC-3: Единый mapper CardResponse.from(Card)

**Уровень:** Code Review + API IT  
**Риск:** R-01 (Score 9 — CRITICAL)  
**Связь:** G-01

#### ATDD-3.1 (STRUCTURAL): BoardService делегирует CardResponse.from(Card)

```gherkin
Given: BoardService.java
When: выполнен поиск вызовов new CardResponse(...) внутри BoardService
Then: таких вызовов нет — все заменены на CardResponse.from(card)
```

**Критерий прохождения:** BoardService не содержит прямого вызова конструктора CardResponse.

#### ATDD-3.2 (STRUCTURAL): CardService делегирует CardResponse.from(Card)

```gherkin
Given: CardService.java
When: выполнен поиск вызовов new CardResponse(...) внутри CardService
Then: таких вызовов нет — все заменены на CardResponse.from(card)
```

**Критерий прохождения:** CardService не содержит прямого вызова конструктора CardResponse.

#### ATDD-3.3 (INTEGRATION): Оба service возвращают идентичные CardResponse

```gherkin
Given: BoardService.getBoard(boardId) и CardService.getCard(cardId) для одной и той же карточки
When: оба ответа содержат CardResponse для этой карточки
Then: creatorId, creatorUsername, assigneeId, assigneeUsername идентичны в обоих ответах
```

**Критерий прохождения:** Единый mapper гарантирует консистентность независимо от точки входа.

---

### 2.4 AC-4: UpdateCardRequest — resetAssignee семантика

**Уровень:** API IT (MockMvc)  
**Риск:** R-03 (Score 4 — MEDIUM)  
**Связь:** G-02, P1-007 (TS-13)

#### ATDD-4.1 (POSITIVE): updateCard с resetAssignee=true — сброс assignee

```gherkin
Given: карточка с assignee = admin2
When: выполнен PUT /api/cards/{cardId} с телом
  {"title": "Same Title", "resetAssignee": true}
Then: статус 200 OK
  And: assigneeId == null
  And: assigneeUsername == null
```

**Критерий прохождения:** resetAssignee=true сбрасывает assignee в null вне зависимости от наличия assigneeId в запросе.

#### ATDD-4.2 (POSITIVE): updateCard без assigneeId — assignee не меняется

```gherkin
Given: карточка с assignee = admin2
When: выполнен PUT /api/cards/{cardId} с телом
  {"title": "Updated Title"} — поле assigneeId отсутствует
Then: статус 200 OK
  And: assigneeId == id admin2 (не изменился)
  And: assigneeUsername == "admin2" (не изменился)
```

**Критерий прохождения:** Jackson десериализация различает «поле не передано» и «явный null»; отсутствующее поле не меняет assignee.

#### ATDD-4.3 (EDGE): updateCard с assigneeId = null И resetAssignee = false

```gherkin
Given: карточка с assignee = admin2
When: выполнен PUT /api/cards/{cardId} с телом
  {"title": "Updated", "assigneeId": null, "resetAssignee": false}
Then: статус 200 OK
  And: assigneeId == id admin2 (не изменился — resetAssignee=false)
```

**Критерий прохождения:** Когда resetAssignee=false и assigneeId=null, assignee не сбрасывается (сохраняется существующий).

---

### 2.5 AC-5: Существующие тесты проходят

**Уровень:** Regression IT  
**Риск:** —  
**Связь:** Story 4.1 integration-ready

#### ATDD-5.1 (REGRESSION): 4 ранее @Disabled теста проходят

```gherkin
Given: V3 миграция применена (creator_id, assignee_id columns exist)
  And: Card entity расширена полями creator (NOT NULL) и assignee (nullable)
  And: @Disabled аннотация снята с 4 тестов:
    - cardCrudAndMoveNormalizeStablePositions
    - invalidMoveDoesNotChangePersistedOrder
    - deletingBoardCascadesColumnsAndCards
    - resourcesOwnedByAnotherAdminAreHidden
When: выполнен запуск KanbanIntegrationTest
Then: все тесты PASS (включая ранее @Disabled)
```

**Критерий прохождения:** `mvn test` — 0 failures, 0 errors.

---

## 3. Integration Scenarios

### 3.1 Установка creator при создании карточки

**Story:** 4.2 (entity-level) + 4.3 (service-level)  
**Риск:** R-05 (Score 6 — HIGH)  
**Уровень:** API IT (MockMvc)

#### ATDD-INT-1.1 (NEGATIVE): createCard без сессии — 401

```gherkin
Given: нет аутентификации
When: выполнен POST /api/cards с валидным телом
Then: статус 401 Unauthorized
```

**Критерий прохождения:** Без токена creator не может быть установлен — запрос отклонён.

#### ATDD-INT-1.2: GET /api/boards/{id} — карточки загружаются с creator/assignee

```gherkin
Given: BoardService.getBoard в @Transactional контексте
When: выполнен GET /api/boards/{boardId}
Then: creator.getUsername() и assignee.getUsername() не вызывают LazyInitializationException
```

**Критерий прохождения:** LAZY fetch работает корректно в рамках @Transactional.

### 3.2 JSON-контракт — совместимость

**Уровень:** API IT (MockMvc)  
**Риск:** R-04 (Score 4 — MEDIUM)

#### ATDD-INT-2.1 (COMPAT): старый запрос createCard без assigneeId — проходит

```gherkin
Given: тело запроса без assigneeId {"title": "Old", "columnId": <id>}
When: выполнен POST /api/cards
Then: статус 201 Created
  And: creatorId NOT NULL
  And: assigneeId == null
```

**Критерий прохождения:** assigneeId опционален — обратная совместимость сохранена.

#### ATDD-INT-2.2 (COMPAT): нестрогое JSON-сравнение — существующие тесты не ломаются

```gherkin
Given: существующий KanbanIntegrationTest с нестрогим сравнением JSON
When: CardResponse содержит 4 новых поля
Then: тесты PASS (новые поля игнорируются сравнением)
```

**Критерий прохождения:** Ручная проверка — ответ содержит creatorId/creatorUsername/assigneeId/assigneeUsername, но старые тесты с partial сравнением не падают.

---

## 4. Pass/Fail Criteria

### 4.1 Фаза pre-commit (локально)

| Критерий | Проверка | Ожидание |
|----------|----------|----------|
| **C-01** | `mvn compile` (backend) | BUILD SUCCESS |
| **C-02** | `npm run lint && npm run build` (frontend, types) | 0 errors, 0 warnings |
| **C-03** | Все P0-сценарии ATDD (MockMvc) | PASS (0 failures) |

### 4.2 Фаза CI (PR)

| Критерий | Проверка | Ожидание |
|----------|----------|----------|
| **C-04** | `mvn test` — KanbanIntegrationTest | 100% PASS |
| **C-05** | P0-006 (TS-06): Board detail cards contain 4 fields | PASS |
| **C-06** | G-01: CardResponse.from(Card) единый mapper | PASS (code review + test) |
| **C-07** | G-02: resetAssignee семантика | PASS |

### 4.3 Quality Gates (из TEA Handoff)

| Gate | Критерий | Метод проверки |
|------|----------|----------------|
| **G-01** | CardResponse.from(Card) — единый mapper; BoardService + CardService делегируют | Code review + ATDD-3.1, ATDD-3.2 |
| **G-02** | UpdateCardRequest различает absent assigneeId vs null (resetAssignee) | ATDD-4.1, ATDD-4.2, ATDD-4.3 |

---

## 5. Тестовые данные (Data Factories)

### 5.1 Предусловия для всех тестов

```java
// Bootstrap ADMIN (существует из V1/V2)
User admin = userRepository.findByUsername("admin").orElseThrow();

// Второй ADMIN для тестов assignee
User admin2 = seedUser("admin2", "password2"); // UserFactory или @Sql

// Доска + колонка
Board board = createBoard("Test Board");
Column todoColumn = getFirstColumn(board, ColumnStatus.TODO);
```

### 5.2 Тестовые helper-методы

```java
private User seedAdminUser(String username, String password) {
    // Использовать passwordEncoder для хеширования пароля
    // Установить роль ADMIN
    // Вернуть сохранённого User
}

private Card createCardWithCreator(String title, Long columnId, User creator) {
    Card card = new Card(title, columnId, creator); // новый конструктор с creator
    return cardRepository.save(card);
}
```

### 5.3 JSON-шаблоны запросов

**createCard без assignee:**
```json
{"title": "Test Card", "columnId": 1}
```

**createCard с assignee:**
```json
{"title": "Assigned Card", "columnId": 1, "assigneeId": 2}
```

**updateCard — сброс assignee:**
```json
{"resetAssignee": true}
```

**updateCard — без assigneeId:**
```json
{"title": "Updated Title"}
```

**updateCard — null assigneeId + reset=false:**
```json
{"assigneeId": null, "resetAssignee": false}
```

---

## 6. Ожидаемые HTTP-ответы

### 6.1 CardResponse (201/200)

```json
{
  "id": 1,
  "boardId": 1,
  "title": "Test Card",
  "description": null,
  "status": "TODO",
  "position": 0,
  "createdAt": "2026-07-22T12:00:00",
  "updatedAt": "2026-07-22T12:00:00",
  "creatorId": 1,
  "creatorUsername": "admin",
  "assigneeId": 2,
  "assigneeUsername": "admin2"
}
```

### 6.2 BoardResponse (с карточками)

Каждая карточка внутри поле `cards[]` содержит те же 4 поля creator/assignee.

---

## 7. Связь со story/AC

| Сценарий ATDD | AC-1 | AC-2 | AC-3 | AC-4 | AC-5 |
|---------------|------|------|------|------|------|
| ATDD-1.1 | ✅ | | | | |
| ATDD-1.2 | ✅ | | | | |
| ATDD-1.3 | ✅ | | | | |
| ATDD-2.1 | | ✅ | | | |
| ATDD-2.2 | | ✅ | | | |
| ATDD-3.1 | | | ✅ | | |
| ATDD-3.2 | | | ✅ | | |
| ATDD-3.3 | | | ✅ | | |
| ATDD-4.1 | | | | ✅ | |
| ATDD-4.2 | | | | ✅ | |
| ATDD-4.3 | | | | ✅ | |
| ATDD-5.1 | | | | | ✅ |

---

## 8. Итоговый план тестирования

| Категория | Количество | Уровень |
|-----------|-----------|---------|
| P0 (Critical) | 9 | API IT (MockMvc) |
| P1 (High) | 3 | API IT (MockMvc) |
| P2 (Medium) | 2 | API IT + код-ревью |
| **Всего** | **14** | |

### Покрытие рисков

| Risk ID | Score | Покрытие |
|---------|-------|----------|
| R-01 | 9 | ATDD-1.1, ATDD-1.2, ATDD-1.3, ATDD-2.1, ATDD-2.2, ATDD-3.1, ATDD-3.2, ATDD-3.3 |
| R-03 | 4 | ATDD-4.1, ATDD-4.2, ATDD-4.3 |
| R-04 | 4 | ATDD-INT-2.1, ATDD-INT-2.2 |
| R-05 | 6 | ATDD-INT-1.1 |
| R-08 | 1 | — (moveCard не меняет assignee) |
| R-10 | 3 | ATDD-5.1 (регрессия) |

---

**Generated by:** Murat (Master Test Architect) — TEA headless flow  
**Workflow:** bmad-testarch-atdd (via bmad-tea@1.0 knowledge fragments)  
**HGSDLC Node:** ai-prepare-atdd (attempt-2)  
**Date:** 2026-07-22
