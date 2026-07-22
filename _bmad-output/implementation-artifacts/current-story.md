---
id: "4.2"
epic: 4
story: 2
key: "4-2-card-entity-dto-поля-creator-assignee-и-единый-mapper"
title: "Card entity + DTO — поля creator/assignee и единый mapper"
status: "ready-for-dev"
baseline_commit: "19e5c2287906973db0532b1275e36fc95312697a"
created: "2026-07-22"
updated: "2026-07-22T18:00"
feature: "Параметры пользователей на карточках (creator + assignee)"
---

# Story 4.2: Card entity + DTO — поля creator/assignee и единый mapper

## Цель

Расширить Card entity полями `creator` (ManyToOne, NOT NULL) и `assignee` (ManyToOne, nullable), а также CardResponse четырьмя полями (creatorId, creatorUsername, assigneeId, assigneeUsername), и вынести единый метод сборки `CardResponse.from(Card)`, чтобы бизнес-логика и read model пользовались общим контрактом.

## Affected service

`backend/` — только Java-уровень (entity, DTO, mapper, BoardService делегирование). Никаких изменений БД (V3 миграция уже выполнена в Story 4.1), никаких изменений фронтенда.

## Scope

- **Card.java**: + поля `creator` (`@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "creator_id", nullable = false)`) и `assignee` (`@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "assignee_id")`, nullable)
- **CardDtos.java**: расширение CardResponse + 4 поля; CreateCardRequest + `assigneeId` (Long, nullable); UpdateCardRequest + `assigneeId` (Long, nullable) + `resetAssignee` (boolean, default false)
- **CardResponse.from(Card)**: единый статический factory-метод в CardDtos
- **BoardService.toCardResponse**: делегирует `CardResponse.from(Card)` вместо дублирования конструктора
- **CardService.toResponse**: делегирует `CardResponse.from(Card)` вместо дублирования конструктора
- **KanbanIntegrationTest.java**: снять `@Disabled` с 4 тестов (они падали из-за NOT NULL на creator_id — теперь entity предоставляет creator)
- Никаких изменений CardController, CardRepository, CardService.create/update/move (логика — Story 4.3)
- Никаких изменений фронтенда

## Acceptance Criteria

### AC-1 (P0-006 → R-01): CardResponse содержит 4 поля creator/assignee
**Given** Card entity расширена полями creator и assignee  
**When** карточка загружается через любой эндпоинт  
**Then** CardResponse содержит `creatorId`, `creatorUsername` (NOT NULL), `assigneeId`, `assigneeUsername` (nullable)

### AC-2 (P0-006 → R-01): Board detail — карточки с creator/assignee
**Given** BoardService.getBoard возвращает карточки  
**When** вызывается получение доски  
**Then** каждая карточка в BoardResponse содержит creatorId/creatorUsername/assigneeId/assigneeUsername

### AC-3 (G-01): Единый mapper CardResponse.from(Card)
**Given** `CardResponse.from(Card)` реализован  
**When** BoardService, CardService или любой другой потребитель собирает CardResponse  
**Then** используется единый метод, а не дублирование конструктора

### AC-4 (G-02): UpdateCardRequest — resetAssignee семантика
**Given** UpdateCardRequest  
**When** JSON содержит `assigneeId: null` и `resetAssignee: true`  
**Then** assignee сбрасывается  
**When** JSON не содержит поля `assigneeId`  
**Then** assignee не меняется (resetAssignee=false по умолчанию)

### AC-5: Существующие тесты проходят
**Given** V3 миграция применена и Card entity расширена  
**When** запускаются существующие KanbanIntegrationTest  
**Then** все тесты проходят (сняты @Disabled с 4 тестов из Story 4.1)

## Implementation Notes

### Архитектурные решения (из architecture-delta §4.1–4.2, §4.8)

1. **`@ManyToOne(fetch = FetchType.LAZY)`** для creator и assignee. `open-in-view=false`, поэтому загрузка должна происходить в рамках `@Transactional` service-метода.
2. **`CardResponse.from(Card)`** — статический factory-метод в `CardDtos.java`. Единая точка сборки DTO из entity. Все потребители (BoardService, CardService) делегируют этому методу.
3. **CreateCardRequest.assigneeId**: `Long` (nullable) — отсутствие/null = не назначать assignee.
4. **UpdateCardRequest**: `Long assigneeId` (nullable) + `boolean resetAssignee` (default false). Если `resetAssignee=true` — сброс в null. Если `assigneeId` передан и не null — установка. Если оба false/не переданы — без изменений.
5. **LAZY fetch**: creator и assignee загружаются в `@Transactional` service. Достаточно для всех сценариев (A-5).

### Card.java — изменения

```java
// Добавить поля:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "creator_id", nullable = false)
private User creator;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")
private User assignee;
```

- Обновить существующий конструктор: добавить параметр `User creator`, установить `assignee = null`
- Добавить геттеры: `getCreator()`, `getAssignee()`
- JPA no-arg constructor остаётся protected
- `creator` NOT NULL — устанавливается при создании, не изменяется (инвариант BR-8)
- `assignee` nullable — управляется через CardService (Story 4.3)

### CardDtos.java — изменения

**CardResponse** — добавить 4 поля в запись (record):
```java
Long creatorId,
String creatorUsername,
Long assigneeId,
String assigneeUsername
```

**Добавить статический factory-метод:**
```java
public static CardResponse from(Card card) {
    return new CardResponse(
        card.getId(),
        card.getBoard().getId(),
        card.getTitle(),
        card.getDescription(),
        card.getStatus(),
        card.getPosition(),
        card.getCreatedAt(),
        card.getUpdatedAt(),
        card.getCreator().getId(),
        card.getCreator().getUsername(),
        card.getAssignee() != null ? card.getAssignee().getId() : null,
        card.getAssignee() != null ? card.getAssignee().getUsername() : null
    );
}
```

**CreateCardRequest** — добавить поле:
```java
@Nullable
Long assigneeId
```

**UpdateCardRequest** — добавить поля:
```java
@Nullable
Long assigneeId,

boolean resetAssignee
```
Примечание: `resetAssignee` должна иметь значение по умолчанию `false` (Jackson `@JsonProperty(defaultValue = "false")` или примитив `boolean` с дефолтом).

### BoardService.java — изменения

Заменить существующий `private CardResponse toCardResponse(Card card)` (строка ~198):
```java
// ДО:
private CardResponse toCardResponse(Card card) {
    return new CardResponse(card.getId(), ...);
}

// ПОСЛЕ — удалить метод, заменить вызовы на CardResponse.from(card):
// В toResponse(Board board): card -> CardResponse.from(card)
```

### CardService.java — изменения

Заменить существующий `private CardResponse toResponse(Card card)` (строка ~138):
```java
// ДО:
private CardResponse toResponse(Card card) {
    return new CardResponse(card.getId(), ...);
}

// ПОСЛЕ — удалить метод, заменить вызовы на CardResponse.from(card)
```

### KanbanIntegrationTest.java — изменения

Снять `@Disabled` с 4 тестов:
- `cardCrudAndMoveNormalizeStablePositions`
- `invalidMoveDoesNotChangePersistedOrder`
- `deletingBoardCascadesColumnsAndCards`
- `resourcesOwnedByAnotherAdminAreHidden`

Каждый тест теперь будет работать, т.к. Card entity предоставляет поля creator/assignee, и новые NOT NULL ограничения удовлетворены.

### Critical implementation requirements

1. **Порядок имплементации**: Card.java → CardDtos.java (+ from метод) → BoardService.toCardResponse → CardService.toResponse → тесты
2. **НЕ трогать CardService.create/update/move** — бизнес-логика установки creator/assignee будет в Story 4.3
3. **creator field обязателен** — все entity операции (save, update) должны устанавливать creator. Для тестов можно использовать существующего bootstrap ADMIN как creator.
4. **LAZY fetch** — creator.getUsername() и assignee.getUsername() вызываются внутри @Transactional service, поэтому LazyInitializationException не возникнет.
5. **Существующие тесты** — нестрогое JSON-сравнение (MockMvc) не сломается от появления новых полей; assigneeId в create/update опционален (обратная совместимость).
6. **`@Disabled` тесты** — после снятия @Disabled тесты могут потребовать установки creator при создании Card в тестовом коде. Сейчас тесты используют прямой `new Card(...)`. Если Card конструктор изменился (добавлен параметр creator), обновить вызовы в тестах или в entity конструкторе.

### Business rules applied

- BR-7: single-user — создатель всегда текущий admin (creator устанавливается из сессии, но логика — в Story 4.3)
- BR-8: creator не изменяется после создания (инвариант на уровне entity: нет setCreator)

### Validation report findings applied

- OQ-1 (решено: единый метод CardResponse.from(Card))
- OQ-4 (решено: resetAssignee boolean флаг)

## Dependencies

| Зависимость | Тип | Статус |
|---|---|---|
| V3 миграция (Story 4.1) — creator_id/assignee_id columns exist | hard dependency | integration-ready ✅ |
| User entity (V1/V2) | assumption | confirmed |
| LAZY fetch + @Transactional достаточны | assumption | A-5 confirmed |
| Существующие тесты не сломаются от новых полей | assumption | A-6 (нестрогое JSON) |

## Test Notes

### P0/P1 тесты (из TEA handoff)

| Test ID | Описание | Risk | AC |
|---|---|---|---|
| P0-006 (TS-06) | Board detail — карточки содержат creator/assignee поля | R-01 (Score 9) | AC-2 |
| G-01 | CardResponse.from(Card) единый mapper реализован; BoardService делегирует | R-01 | AC-3 |
| G-02 | UpdateCardRequest различает absent vs null (resetAssignee) | R-03 | AC-4 |
| IT-11 | BoardService.getBoard: карточки содержат creator/assignee | R-01 | AC-2 |

### Testability concerns (из test-design-architecture.md)

- **R-01 (Score 9) — CRITICAL**: Рассинхронизация BoardService.toCardResponse — карточки на Board detail без creator/assignee.
  - Mitigation: единый метод `CardResponse.from(Card)`; BoardService + CardService делегируют.
  - Verification: P0-006 (TS-06): Board detail cards содержат 4 поля.
  - Owner: Backend, pre-implementation.
- **R-03 (Score 4) — MEDIUM**: Jackson null vs absent в UpdateCardRequest.assigneeId.
  - Mitigation: `resetAssignee` boolean флаг с default false.
  - Verification: IT на оба сценария (P1-007/TS-13).

### Quality gates (из TEA Handoff)

| Gate | Критерий | Оwner |
|---|---|---|
| G-01 | CardResponse.from(Card) единый mapper; BoardService.toCardResponse делегирует | Backend |
| G-02 | UpdateCardRequest различает absent assigneeId vs null (resetAssignee) | Backend |

### Risk priorities

| Risk ID | Score | Category | Mitigation |
|---|---|---|---|
| R-01 | 9 | TECH | Единый CardResponse.from(Card); BoardService + CardService делегируют |
| R-03 | 4 | TECH | resetAssignee boolean флаг с default false |

### Test approach

- Существующие KanbanIntegrationTest — снять @Disabled, убедиться что все проходят
- Визуальная проверка JSON-ответа CardResponse содержит 4 новых поля
- Проверка Board detail: каждая карточка содержит creatorId/creatorUsername/assigneeId/assigneeUsername
- Нестрогое сравнение JSON — существующие тесты не должны сломаться

## Rollback Notes

- **Backend**: откатить изменения entity, DTO, mapper
- V3 миграция остаётся (часть Story 4.1, уже в integration-ready)
- Вернуть `@Disabled` на 4 теста KanbanIntegrationTest
- **Rollback безопасен**: schema уже готова (V3), откат Java-кода не требует миграции

## Architecture Delta References

- **Архитектурная дельта**: `_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/architecture-delta.md`
  - §4.1 Card entity — расширение (строка 132–147)
  - §4.2 DTO — расширение CardResponse (строка 150–158)
  - §4.3 CardService — общий mapper (строка 173–176)
  - §4.8 AD-8 уточнение (строка 264–266)
  - §3.1 Affected services/modules (таблица)
  - §3.2 API/contracts changes (таблица)
  - §3.6 Regression areas (RR-1, RR-2)
  - §5.1 Implementation requirements (REQ-2, REQ-3, REQ-4, REQ-5)
  - §5.2 Dependencies/order

## Test Design References

- **test-design-architecture.md**: §Risk Assessment (R-01, R-03), §Mitigation Plans (R-01), §Testability Concerns (B-01)
- **test-design-qa.md**: P0-006 (TS-06), §Risk Assessment (R-01, R-03), §P0 тесты
- **TEA Handoff**: G-01, G-02, R-01, R-03

## Open Questions

Отсутствуют. Все OQ решены в architecture delta. Новых вопросов по entity/DTO/mapper не выявлено.

## Assumptions

| ID | Assumption | Обоснование |
|---|---|---|
| A-2 | User entity и UserRepository уже существуют (V1/V2) | PRD addendum §10; project-context |
| A-5 | LAZY fetch creator/assignee достаточен (загрузка в @Transactional) | PRD addendum §10; open-in-view=false |
| A-6 | Существующие тесты не сломаются от новых полей (нестрогое сравнение) | PRD addendum §10 |

## Tasks/Subtasks

### Implementation Tasks

- [x] Card.java — добавить поля creator (ManyToOne, NOT NULL) и assignee (ManyToOne, nullable)
- [x] CardDtos.java — расширить CardResponse + 4 поля; CreateCardRequest + assigneeId; UpdateCardRequest + assigneeId + resetAssignee
- [x] CardDtos.java — добавить CardResponse.from(Card) статический factory-метод
- [x] BoardService.java — заменить toCardResponse на делегирование CardResponse.from(Card)
- [x] CardService.java — заменить toResponse на делегирование CardResponse.from(Card)
- [x] KanbanIntegrationTest.java — снять @Disabled с 4 тестов
- [x] Запустить `mvn test` — проверить что все тесты проходят

## File List

- `backend/src/main/java/com/bmad/todolist/card/Card.java` — **MODIFY** — + поля creator/assignee, обновлён конструктор, геттеры
- `backend/src/main/java/com/bmad/todolist/card/CardDtos.java` — **MODIFY** — +4 поля в CardResponse + from() метод + assigneeId/resetAssignee в request'ах
- `backend/src/main/java/com/bmad/todolist/board/BoardService.java` — **MODIFY** — toCardResponse → CardResponse.from()
- `backend/src/main/java/com/bmad/todolist/card/CardService.java` — **MODIFY** — toResponse → CardResponse.from(), + UserRepository
- `backend/src/main/resources/application.yml` — **MODIFY** — + Jackson deserialization config
- `backend/src/test/java/com/bmad/todolist/kanban/KanbanIntegrationTest.java` — **MODIFY** — снять @Disabled с 4 тестов
- `backend/src/test/java/com/bmad/todolist/migration/V3MigrationIntegrationTest.java` — **MODIFY** — fix identity counter conflict (explicit id)

## Change Log

| Date | Change | Author |
|---|---|---|
| 2026-07-22 | Story 4.2 создана: entity + DTO + единый mapper | Amelia |
| 2026-07-22 | Реализованы entity поля creator/assignee, DTO расширение, единый CardResponse.from(), удалены дублирующие toResponse методы | Amelia |

## Status

**review** — реализована, ожидает code review
