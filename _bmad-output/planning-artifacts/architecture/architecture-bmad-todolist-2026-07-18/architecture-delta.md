---
title: Architecture Delta — Параметры пользователей на карточках
parent_spine: _bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md
parent_spine_status: final
status: draft
feature: creator + assignee on cards
altitude: feature
paradigm: layered REST API + component SPA (inherited)
created: 2026-07-22
updated: 2026-07-22
---

# Architecture Delta — Параметры пользователей на карточках (creator + assignee)

## 1. Sources and scope

### Driving inputs

| Source | Location | Role |
|--------|----------|------|
| PRD (base) | `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md` | AS-IS product definition; FR-11, FR-12 baseline |
| PRD Addendum | `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md` | Feature delta: FR-11/FR-12 extension, BR-1–BR-8, AC-1–AC-8 |
| Validation Report | `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/validation-report.md` | 3 medium findings (OQ-4, RR-1/OQ-1, OQ-2), 3 low findings |
| PRD Validation Summary | inline (PASS verdict) | No blocking findings |
| Product Impact Assessment | inline (§1–§7 PIA) | Affected scenarios, NFR concerns, regression hypotheses |
| ARCHITECTURE-SPINE.md (parent) | `_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md` | Binding inherited invariants (AD-1–AD-12), deferred items |
| Project Context | `_bmad-output/project-context.md` | Brownfield conventions: stack, style, testing, anti-patterns |

### Explicit non-goals (from addendum §7 + PIA-4–PIA-6)

- Нет страницы управления пользователями, регистрации, смены пароля
- Нет новых ролей (только ADMIN)
- Нет изменения Board / BoardResponse / BoardSummary
- Нет изменения эндпоинтов boards, columns, auth
- Нет фильтрации/сортировки по assignee на фронтенде
- Нет отдельного UI-экрана — только расширение существующих компонентов
- Нет уведомлений при назначении assignee
- Нет истории изменений assignee (audit log)
- Нет каскадного поведения при удалении User (single-user v1)
- Нет смены creator после создания карточки
- FR-14 (move) — assignee не меняется при перемещении

## 2. Inherited Invariants

Следующие AD из родительского ARCHITECTURE-SPINE.md являются binding read-only constraints для данной дельты. ID сохранены без перенумерации. Дельта не вводит противоречий с inherited AD; если дельта затрагивает область AD, изменения явно перечислены в §4 как уточнение правила.

| AD | Суть | Применимость к дельте |
|----|------|-----------------------|
| AD-1 | Layering + package shape | Карточки остаются в `card/` пакете. Creator/assignee поля — расширение `Card` entity |
| AD-2 | Dependency direction | Без изменений. FE → BE через `/api` |
| AD-3 | Authn/Authz + JWT | Creator заполняется из `@AuthenticationPrincipal`. Assignee валидируется `hasRole('ADMIN')` |
| AD-4 | Three-column invariant | Без изменений |
| AD-5 | Ownership + write locking | `assignee` не добавляет прав доступа. Pessimistic lock на Board покрывает конкурентные обновления assignee |
| AD-6 | Card position + status mutation | **Уточняется:** assignee — новое мутируемое поле, управляется `CardService` (единственный writer). Move не трогает assignee |
| AD-7 | Schema ownership | V3 — новая Flyway миграция. Существующие V1/V2 не редактируются |
| AD-8 | Error + DTO contract | **Уточняется:** CardResponse расширяется 4 полями. BoardService.toCardResponse должен синхронизироваться. Ошибки assignee — 400 Bad Request через `BadRequestException` |
| AD-9 | FE HTTP single path | Новый `GET /api/users` проходит через `apiRequest` в `client.ts` |
| AD-10 | Deploy / CORS / ops | Без изменений |
| AD-11 | Aggregate boundaries | **Уточняется:** новый `GET /api/users` — read-only, не нарушает агрегат. `CardService` — единственный writer Card (включая assignee) |
| AD-12 | Board read-model composition | BoardResponse.columns[].cards[] включает новые поля CardResponse — формат не меняется |

## 3. Technical Impact Map

### 3.1 Affected services/modules

| Модуль | Компонент | Характер изменения |
|--------|-----------|-------------------|
| **Backend: card** | `Card.java` | + поля `creator` (ManyToOne User, NOT NULL), `assignee` (ManyToOne User, nullable) |
| | `CardDtos.java` | + `creatorId`, `creatorUsername`, `assigneeId`, `assigneeUsername` в CardResponse; + `assigneeId` в CreateCardRequest, UpdateCardRequest |
| | `CardService.java` | + установка `creator` из principal при create; + обработка/валидация `assigneeId` при create/update; валидация существования User + роль ADMIN |
| | `CardController.java` | Без изменений |
| | `CardRepository.java` | Без изменений |
| **Backend: board** | `BoardService.java` | `toCardResponse` — синхронизировать с CardResponse (делегировать `CardService.toResponse` или использовать общий mapper) |
| **Backend: user** | `UserRepository.java` | + опционально: метод `findByRole(RoleName ADMIN)` |
| **Backend: new** | `UserController.java` (новый) | + `GET /api/users` — возвращает `[{id, username}]` всех ADMIN |
| | `UserDtos.java` (новый) | DTO для ответа (id, username) |
| **Backend: migration** | `V3__add_card_users.sql` (новый) | + `creator_id` (NOT NULL), `assignee_id` (nullable), FK → users(id) |
| **Backend: tests** | `KanbanIntegrationTest.java` | + тесты creator/assignee при create, update; + тесты валидации assigneeId |
| | `UserIntegrationTest.java` (новый) | + тесты GET /api/users (200, 401, 403) |
| **Frontend** | `api/kanban.ts` | + поля CardResponse; + `assigneeId` в createCard/updateCard |
| | `api/users.ts` (новый) | Модуль для `GET /api/users` |
| | `KanbanCard.tsx` | + отображение `creatorUsername`, `assigneeUsername` |
| | `CardForm.tsx` | + опциональное поле выбора assignee (select/dropdown из списка users) |

### 3.2 API/contracts changes

| Endpoint | Method | Change | Backward compatible |
|----------|--------|--------|---------------------|
| `/api/cards` | POST | CreateCardRequest + `assigneeId` (nullable) | Да (опциональное поле) |
| `/api/cards/{id}` | PUT | UpdateCardRequest + `assigneeId` (nullable, семантика absent vs null) | Да (опциональное поле) |
| `/api/cards` | GET | CardResponse + 4 поля (creatorId, creatorUsername, assigneeId, assigneeUsername) | Да (добавление полей) |
| `/api/cards/{id}` | GET | CardResponse + 4 поля | Да |
| `/api/users` | GET (новый) | Возвращает `[{id, username}]` всех ADMIN | N/A (новый) |

**Решение по OQ-4 (null vs absent assigneeId):**
- Jackson десериализует отсутствующее `Long` поле как null. Для различения:
  - `CreateCardRequest.assigneeId`: `Long` (nullable) — отсутствие/null = не назначать. Достаточно.
  - `UpdateCardRequest.assigneeId`: используем `Optional<Long>` в DTO (альтернатива — кастомный десериализатор или отдельный флаг). Для идиоматичности Spring: в `UpdateCardRequest` добавляем поле `assigneeId` типа `Long` + поле `resetAssignee` типа `boolean` (default false). Если `resetAssignee=true` — сброс в null. Если `assigneeId` передан и не null — установка. Если оба false/не переданы — без изменений.

**Решение по OQ-3 (HTTP статус для некорректного assigneeId):** 400 Bad Request через `BadRequestException` — единообразно с существующей моделью ошибок.

### 3.3 Data/DB/ownership

| Изменение | Детали |
|-----------|--------|
| `cards.creator_id` | BIGINT NOT NULL, FK → users(id) |
| `cards.assignee_id` | BIGINT nullable, FK → users(id) |
| Индексы | + индекс на `creator_id`; + индекс на `assignee_id` (создать сразу, OQ-5 → решение A) |
| Data ownership | creator и assignee — ссылки на User. User не является aggregate root для карточки; ownership карточки остаётся через Board.author |
| Flyway | `V3__add_card_users.sql` — ALTER TABLE cards ADD COLUMN + ADD CONSTRAINT + UPDATE + SET NOT NULL |

### 3.4 Events/queues

Без изменений. В проекте нет очередей и событий. Добавление assignee — синхронная операция в рамках HTTP-запроса.

### 3.5 Integrations

Новые или изменяемые интеграции отсутствуют. `GET /api/users` — внутренний эндпоинт, не интеграция.

### 3.6 Regression areas

| ID | Риск | Причина | Митигация в дельте |
|----|------|---------|--------------------|
| RR-1 | BoardService.getBoard возвращает карточки без creator/assignee | BoardService.toCardResponse дублирует конструктор CardResponse | Вынести общий mapper (CardService.toResponse); BoardService делегирует его |
| RR-2 | moveCard возвращает CardResponse без creator/assignee | move вызывает CardService.move — должен использовать общий mapper | Убедиться, что CardService.move возвращает CardResponse через единый метод |
| RR-3 | FE updateCard не передаёт assigneeId | Обратная совместимость | assigneeId опционален; отсутствие = без изменений assignee |
| RR-4 | Существующие тесты card CRUD падают | Появление новых полей в CardResponse | Нестрогое JSON-сравнение не ломается; если тесты используют строгую сериализацию — обновить ожидания |
| RR-5 | Drag-and-drop optimistic update расходится | assignee не меняется при move | Митигировано: move не трогает assignee |

## 4. Proposed Architecture Delta

### 4.1 Card entity — расширение

```diff
  @Entity
  @Table(name = "cards")
  public class Card {
      // existing: id, title, description, status, position, board_id
+     @ManyToOne(fetch = FetchType.LAZY)
+     @JoinColumn(name = "creator_id", nullable = false)
+     private User creator;
+
+     @ManyToOne(fetch = FetchType.LAZY)
+     @JoinColumn(name = "assignee_id")
+     private User assignee;
  }
```

### 4.2 DTO — расширение CardResponse

```diff
  public record CardResponse(
      Long id, String title, String description,
      ColumnStatus status, int position, Long boardId,
+     Long creatorId, String creatorUsername,
+     Long assigneeId, String assigneeUsername
  ) {}
```

### 4.3 CardService — новая логика

**create:**
- Установка `creator` из `@AuthenticationPrincipal UserPrincipal` (получение User через UserRepository)
- Если `assigneeId` передан: валидация (User exists + role ADMIN) → установка assignee
- Если `assigneeId` не передан / null: assignee = null

**update:**
- Если `assigneeId` присутствует (не null): валидация + установка
- Если `resetAssignee = true`: сброс в null
- Если `assigneeId` отсутствует и `resetAssignee = false`: assignee не меняется
- creator не изменяется (инвариант BR-8)

**Общий mapper для CardResponse:**
- Вынести статический factory-метод `CardResponse.from(Card card)` в CardDtos (или отдельный `CardMapper.java`)
- `BoardService.toCardResponse` заменяется вызовом `CardResponse.from(card)`

### 4.4 Новый эндпоинт GET /api/users

**Решение по OQ-2:** Добавить `GET /api/users`, защищённый `@PreAuthorize("hasRole('ADMIN')")`. Возвращает `[{id, username}]` для всех пользователей с ролью ADMIN.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    @GetMapping
    public List<UserSummary> getUsers() {
        return userRepository.findAllByRole(RoleName.ADMIN)
            .stream()
            .map(u -> new UserSummary(u.getId(), u.getUsername()))
            .toList();
    }
}
```

Требования безопасности (из validation finding OQ-2):
- Доступен только ADMIN (401 без токена, 403 без роли ADMIN)
- Возвращает только `id` + `username` (без паролей, email и т.д.)
- Rate limiting не требуется (single-user v1)

### 4.5 Flyway V3 — миграция

```sql
-- V3__add_card_users.sql
ALTER TABLE cards
    ADD COLUMN creator_id BIGINT,
    ADD COLUMN assignee_id BIGINT;

-- Backfill: все существующие карточки получают creator = Board.author
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

### 4.6 Frontend — delta

**KanbanCard.tsx:**
```diff
  <div className="kanban-card">
    <h4>{card.title}</h4>
    {card.description && <p>{card.description}</p>}
+   <div className="card-meta">
+     <span className="card-creator">создал: {card.creatorUsername}</span>
+     {card.assigneeUsername && (
+       <span className="card-assignee">исполнитель: {card.assigneeUsername}</span>
+     )}
+   </div>
  </div>
```

**CardForm.tsx:**
- Добавить выпадающий список для выбора assignee (загружается через `GET /api/users` при открытии формы)
- Опциональное поле; по умолчанию не выбран (assignee = null)
- При редактировании: предзаполнен текущим assignee

**api/users.ts:**
```typescript
export interface UserSummary {
  id: number;
  username: string;
}

export async function getUsers(): Promise<UserSummary[]> {
  return apiRequest<UserSummary[]>('/api/users');
}
```

### 4.7 AD-6 — уточнение

Добавить в AD-6 правило: "`CardService` является единственным writer полей `creator` и `assignee`. `creator` устанавливается однократно при создании и не изменяется. `assignee` мутируется через create/update; moveCard не меняет assignee."

### 4.8 AD-8 — уточнение

Добавить в AD-8: "Карточка для сборки CardResponse — единый метод `CardResponse.from(Card)` в `CardDtos`. BoardService и любые другие потребители используют этот метод, а не дублируют конструктор."

### 4.9 AD-11 — уточнение

Добавить в AD-11: "Новый эндпоинт `GET /api/users` в `user/` пакете является read-only и не нарушает aggregate boundaries."

## 5. Downstream constraints

### 5.1 Implementation requirements

| # | Требование | Компонент |
|---|-----------|-----------|
| REQ-1 | Создать V3 Flyway миграцию с backfill creator_id из Board.author | Backend |
| REQ-2 | Добавить поля creator/assignee в Card entity (Lazy ManyToOne) | Backend |
| REQ-3 | Расширить CardDtos: CardResponse + 4 поля; CreateCardRequest + assigneeId; UpdateCardRequest + assigneeId + resetAssignee | Backend |
| REQ-4 | Вынести единый метод сборки CardResponse (CardResponse.from или CardMapper) | Backend |
| REQ-5 | Обновить BoardService.toCardResponse — делегировать единому методу | Backend |
| REQ-6 | CardService.create: установка creator из principal, обработка assignee | Backend |
| REQ-7 | CardService.update: обработка assignee (установка/сброс/без изменений) | Backend |
| REQ-8 | Валидация assigneeId: существование User + роль ADMIN → 400 | Backend |
| REQ-9 | Создать GET /api/users (UserController + UserDtos) | Backend |
| REQ-10 | UserRepository: метод findAllByRole(RoleName) | Backend |
| REQ-11 | FE: расширить типы в api/kanban.ts | Frontend |
| REQ-12 | FE: создать api/users.ts с getUsers() | Frontend |
| REQ-13 | FE: KanbanCard — отображение creatorUsername + assigneeUsername | Frontend |
| REQ-14 | FE: CardForm — выпадающий список выбора assignee | Frontend |

### 5.2 Dependencies / order

```
V3 migration (REQ-1) → Card entity (REQ-2) → DTOs (REQ-3) → mapper (REQ-4)
  → CardService (REQ-6, REQ-7, REQ-8) + BoardService update (REQ-5)
  → GET /api/users (REQ-9, REQ-10)
  → FE types (REQ-11) → FE api/users (REQ-12) → KanbanCard (REQ-13) → CardForm (REQ-14)
```

### 5.3 Contract / migration tests

| Тест | Описание |
|------|----------|
| IT-1 | createCard без assignee: creator = current user, assignee = null |
| IT-2 | createCard с assignee: creator + assignee заполнены |
| IT-3 | createCard с несуществующим assigneeId: 400 |
| IT-4 | createCard с assigneeId не-ADMIN: 400 |
| IT-5 | updateCard: смена assignee |
| IT-6 | updateCard: сброс assignee (resetAssignee=true) |
| IT-7 | updateCard: без assigneeId — assignee не меняется |
| IT-8 | GET /api/users: 200 (ADMIN), возвращает список |
| IT-9 | GET /api/users: 401 без токена |
| IT-10 | Миграция V3: creator_id = Board.author для существующих карточек |
| IT-11 | BoardService.getBoard: карточки содержат creator/assignee |

### 5.4 NFR / Security

| # | NFR | Митигация |
|---|-----|-----------|
| NFR-1 | assigneeId валидируется на существование User + role ADMIN | 400 BadRequestException |
| NFR-2 | GET /api/users — только ADMIN (401/403) | @PreAuthorize("hasRole('ADMIN')") |
| NFR-3 | LAZY fetch для creator/assignee; загрузка в @Transactional | open-in-view=false |
| NFR-4 | NOT NULL на creator_id | FK + set not null в V3 |
| NFR-5 | Индексы на creator_id / assignee_id | Создаются в V3 |
| NFR-6 | assignee не получает прав на карточку ({Board.author}) | BR-5, не меняется |

## 6. Delivery mechanics

### 6.1 Feature flags

Не требуются. Объём изменения мал и атомарен: включение всех изменений в один PR. Single-user — низкий риск.

### 6.2 Migration / backfill / compatibility

- Flyway V3: атомарная миграция с backfill. Все существующие карточки получают `creator_id` = `Board.author_id`
- Backward compatible API: новые поля в CardResponse опциональны для чтения; assigneeId в create/update опционален
- FE: старый UI без обновления не сломается (новые поля игнорируются); функциональность assignee будет недоступна до деплоя FE

### 6.3 Rollout

- Развернуть backend (включая V3 миграцию) → развернуть frontend
- На время между деплоями FE может не видеть assignee UI, но API обратно совместимо

### 6.4 Rollback

- Backend: откатить V3 через `Flyway undo` (если настроено) или через `V3.1__revert_add_card_users.sql`
- Frontend: откатить FE до предыдущей версии
- Rollback безопасен: creator_id NOT NULL не снимается без даунтайма, но assignee_id nullable позволяет просто игнорировать

### 6.5 Observability / alerting

- Health-эндпоинт не меняется
- Ошибки валидации assigneeId — через существующий ApiExceptionHandler (400)
- Метрики/логи — существующий механизм Spring Boot Actuator (без изменений)
- Алерты не добавляются

## 7. Risks / assumptions / open_questions

### Assumptions

| ID | Assumption | Обоснование |
|----|-----------|-------------|
| A-1 | В системе не больше одного ADMIN (single-user v1) | PRD addendum §10; PIA-1 |
| A-2 | User entity и UserRepository уже существуют (V1/V2) | PRD addendum §10; project-context |
| A-3 | Board.author NOT NULL; используется для backfill | PRD addendum §10; existing model |
| A-4 | Pessimistic lock на Board (AD-5) покрывает конкурентные обновления assignee | PRD addendum §10; AD-5 |
| A-5 | LAZY fetch creator/assignee достаточен (загрузка в @Transactional) | PRD addendum §10; open-in-view=false |
| A-6 | Существующие тесты не сломаются от новых полей (нестрогое сравнение) | PRD addendum §10 |
| A-7 | GET /api/users будет использоваться только для выпадающего списка assignee | OQ-2 resolution |

### Risks

| ID | Risk | Severity | Mitigation |
|----|------|----------|------------|
| R-1 | Рассинхронизация BoardService.toCardResponse | High | Единый mapper (REQ-4, REQ-5) |
| R-2 | Backfill creator_id на больших данных | Low | Текущий объём данных незначительный |
| R-3 | Jackson десериализация assigneeId (null vs absent) | Medium | Использовать Optional<Long> или resetAssignee флаг (REQ-3) |
| R-4 | FE optimistic update после move расходится с assignee на сервере | None | move не меняет assignee |

### Open questions

Все OQ из addendum §9 решены в данной дельте. Новых открытых вопросов не выявлено.

| OQ | Решение |
|----|---------|
| OQ-1 (сборка CardResponse) | Единый метод CardResponse.from(Card) в CardDtos. BoardService делегирует |
| OQ-2 (GET /api/users) | Добавить эндпоинт, защищённый ADMIN. Возвращает id + username |
| OQ-3 (HTTP статус) | 400 BadRequestException |
| OQ-4 (null vs absent) | CreateCardRequest: Long assigneeId (nullable). UpdateCardRequest: Long assigneeId + boolean resetAssignee |
| OQ-5 (индексы) | Создать индексы в V3 сразу |
| OQ-6 (cross-board assignee) | Любой ADMIN может быть assignee (без проверки прав доски). BR-5 |

## 8. Spine ADR updates

После approve дельты следующие изменения должны быть внесены в `ARCHITECTURE-SPINE.md`:

### AD-6 — уточнить правило

В AD-6 добавить:
> "`CardService` — исключительный writer полей `creator` и `assignee`. Creator устанавливается однократно при create и не изменяется. Assignee мутируется через create/update; moveCard не меняет assignee."

### AD-8 — уточнить DTO contract

В AD-8 добавить:
> "Сборка CardResponse — через единый метод `CardResponse.from(Card)` (или `CardMapper`). BoardService и любые потребители делегируют этому методу, не дублируя конструктор. Новый эндпоинт `GET /api/users` возвращает `[{id, username}]`."

### AD-11 — уточнить aggregate boundaries

В AD-11 добавить:
> "`GET /api/users` в пакете `user/` — read-only; не нарушает aggregate boundaries."

### ER-диаграмма — расширить Card

В структурной seed-диаграмме (раздел Structural Seed) расширить `CARD`:
```
  CARD {
    string status
    int position
    bigint creator_id    # NEW: FK → users(id), NOT NULL
    bigint assignee_id   # NEW: FK → users(id), nullable
  }
```

### Capability → Architecture Map — добавить строку

| Просмотр пользователей (assignee picker) | `user/`, `api/users.ts` | AD-3, AD-8, AD-11 |

### Deferred — снять "Unifying BoardService/CardService CardResponse mapping"

Удалить из Deferred (решено в данной дельте).
