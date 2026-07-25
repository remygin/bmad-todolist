---
title: System-Level QA Test Design — Назначение исполнителя и создателя на карточку
project: bmad-todolist
status: draft
design_mode: system-level
created: 2026-07-23
updated: 2026-07-23
parent_test_phase: 3
sources:
  - _bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md
  - _bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-23/architecture-delta.md
  - _bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md
  - _bmad-output/test-artifacts/test-design-architecture.md
---

# System-Level QA Test Design

## 1. Test Objectives

### 1.1 Primary Objectives

| # | Objective | Validates | Risk |
|---|-----------|-----------|------|
| O-1 | Access Control: только ADMIN мутирует карточки/доски; assignee — только чтение | BP-6, BP-7, BP-8, AD-3, AC-6.3, AC-6.4 | R-TA-1 (Critical) |
| O-2 | Data Isolation: assignee видит только доски, где назначен; чужие доски — 404 | BP-11, AC-3.1, AC-3.2, AD-5 | R-TA-2 (Critical) |
| O-3 | Data Integrity: creator_id NOT NULL; assignee корректно устанавливается/снимается | BP-1, BP-2, AC-1.1, AC-1.2, AC-2.1, AC-2.2 | R-TA-3 (High) |
| O-4 | Atomicity: создание карточки + выдача доступа — одна транзакция | BP-4, IR-3, NFR-4 | R-TA-4 (High) |
| O-5 | Frontend Role Adaptation: assignee не видит UI мутаций | AC-6.3, AC-6.4, AC-5.3 | R-TA-6 (High) |

### 1.2 Secondary Objectives

| # | Objective | Validates | Risk |
|---|-----------|-----------|------|
| O-6 | Идемпотентность повторного назначения | BP-10, EC-8, IR-11 | R-TA-8 (Low) |
| O-7 | Move-операция не меняет assignee | OQ-10, CT-9, IR-10 | — |
| O-8 | Avatar-компонент: инициалы, placeholder, размеры | C-8, AC-5.2, IR-7 | — |
| O-9 | Снятие assignee: доступ сохраняется, если есть другие карточки | EC-2, BP-5 | R-TA-5 (Medium) |
| O-10 | Delete board: FK CASCADE работает корректно | RGR-8, AD-7 | — |

---

## 2. Coverage Matrix

### 2.1 Functional Requirements Coverage

| FR/AC/BP | Тест-сценарий | Уровень | Risk |
|----------|--------------|---------|------|
| AC-1.1 | POST /api/cards без assigneeId → creator = current user, assignee = null | IT | Critical |
| AC-1.2 | POST /api/cards с валидным assigneeId → assignee получает доступ | IT | Critical |
| AC-1.3 | POST /api/cards с несуществующим assigneeId → 404 | IT | Critical |
| AC-2.1 | PUT /api/cards/{id} с assigneeId: null → снятие назначения | IT | High |
| AC-2.2 | PUT /api/cards/{id} с новым assigneeId → смена + доступ | IT | High |
| AC-3.1 | GET /api/boards/{id} для assignee → 200 | IT | Critical |
| AC-3.2 | GET /api/boards/{id} для не-участника → 404 | IT | Critical |
| AC-3.3 | GET /api/boards — доски автора ИЛИ assignee | IT | Critical |
| AC-4.1 | EXISTS-подзапрос без board_members | IT | Medium |
| AC-5.1 | FE: Card отображает creator username | FE | Medium |
| AC-5.2 | FE: Card отображает Avatar assignee / placeholder | FE | Low |
| AC-5.3 | FE: CardForm dropdown выбора assignee | FE | Medium |
| AC-5.4 | FE: Dropdown грузит GET /api/users | FE | Medium |
| AC-6.1 | FE: Assignee видит Dashboard со своими досками | FE / E2E | Critical |
| AC-6.2 | FE: Assignee видит доску целиком (все карточки) | FE / E2E | High |
| AC-6.3 | FE: Assignee НЕ видит кнопки create/edit/delete | FE | Critical |
| AC-6.4 | FE: Assignee НЕ видит UI управления доской/колонками | FE | Critical |
| BP-1 | creator_id NOT NULL — каждая карточка имеет создателя | IT | Critical |
| BP-2 | assignee_id nullable | IT | Medium |
| BP-3 | Только ADMIN создаёт/редактирует карточки | IT | Critical |
| BP-4 | Назначение assignee → доступ ко всей доске | IT | Critical |
| BP-5 | Снятие последнего assignee → доступ теряется | IT | High |
| BP-9 | creator фиксируется при создании, не меняется | IT | Medium |
| BP-10 | Повторное назначение идемпотентно | IT | Low |

### 2.2 Edge Cases Coverage

| EC | Тест-сценарий | Уровень | Risk |
|----|--------------|---------|------|
| EC-1 | ADMIN создаёт карточку и назначает себя assignee → creator = assignee | IT | Medium |
| EC-2 | Снятие assignee c одной карточки, другая остаётся → доступ сохранён | IT | High |
| EC-3 | Два ADMIN назначают друг друга | IT | Low |
| EC-4 | Assignee аккаунт удалён → SET NULL | IT | Medium |
| EC-5 | Массовое назначение (100 карточек одному assignee) | IT | Low |
| EC-6 | Назначение несуществующего пользователя → 404 | IT | Critical |
| EC-7 | Снятие assignee через null → корректно | IT | High |
| EC-8 | Повторное назначение того же пользователя → идемпотентно | IT | Low |

---

## 3. Risk-Based Prioritization

### 3.1 P0 (Critical — блокирует релиз)

| # | Сценарий | Причина |
|---|----------|---------|
| P0-1 | Assignee получает доступ к мутациям (create/edit/delete/move карточки, управление доской) | Нарушение безопасности BP-6, BP-7 |
| P0-2 | Assignee видит чужие доски (не те, где назначен) | Утечка данных |
| P0-3 | Не-участник получает 200 на GET /api/boards/{id} чужой доски | Утечка данных |
| P0-4 | CREATE card без assignee → creator не установлен (null) | Нарушение BP-1, AC-1.1 |
| P0-5 | CREATE card с assignee → assignee не получает доступ | Нарушение BP-4 |
| P0-6 | FE: assignee видит UI create/edit/delete | Нарушение AC-6.3, AC-6.4 |

### 3.2 P1 (High — тестировать обязательно)

| # | Сценарий | Причина |
|---|----------|---------|
| P1-1 | UPDATE card — смена assignee не синхронизирует доступ | Нарушение AC-2.2 |
| P1-2 | Снятие assignee c последней карточки — доступ не теряется | Нарушение BP-5 |
| P1-3 | CREATE card в транзакции: карточка создана, assignee доступ не выдан | Нарушение IR-3 |
| P1-4 | Существующие тесты не проходят из-за NOT NULL creator_id | CI blocker |
| P1-5 | GET /api/users доступен assignee (403 expected) | Нарушение AD-13 |
| P1-6 | FE BoardPage не различает ADMIN vs assignee | Нарушение AC-6.x |

### 3.3 P2 (Medium — тестировать при возможности)

| # | Сценарий |
|---|----------|
| P2-1 | Avatar-компонент: инициалы из username, серый фон, placeholder |
| P2-2 | CardForm: dropdown assignee загружает getUsers() |
| P2-3 | FE: Card отображает creator username read-only |
| P2-4 | GET /api/users: ADMIN 200, assignee 403 |

### 3.4 P3 (Low — deferred)

| # | Сценарий |
|---|----------|
| P3-1 | Идемпотентность повторного назначения assignee |
| P3-2 | Move-операция не меняет assignee |
| P3-3 | Delete board + FK CASCADE |
| P3-4 | GET /api/users возвращает пустой массив при 0 результатах (кроме ADMIN) |

---

## 4. Detailed Test Scenarios

### 4.1 Backend: Access Control (P0)

**TS-AC-01: Assignee не может мутировать**

```
Given: существующая board с card, где assignee = user_B
When:  user_B отправляет POST /api/cards (или PUT/DELETE)
Then:  403 Forbidden
```

**TS-AC-02: Не-участник не видит доску**

```
Given: board создана user_A
When:  user_B (не assignee) отправляет GET /api/boards/{boardId}
Then:  404 Not Found
```

**TS-AC-03: Assignee видит только свои доски в списке**

```
Given: board_1: user_A + card c assignee = user_B
       board_2: user_A (без карточек для user_B)
When:  user_B отправляет GET /api/boards
Then:  200, в списке только board_1
```

### 4.2 Backend: Creator/Assignee Integrity (P0-P1)

**TS-CR-01: Создание карточки без assignee**

```
Given: user_A (ADMIN) аутентифицирован
When:  POST /api/cards { title: "Test", status: "TODO" }  (без assigneeId)
Then:  201, card.creator.id = user_A.id, card.assignee = null
```

**TS-CR-02: Создание карточки с assignee**

```
Given: user_A (ADMIN) аутентифицирован, user_B существует
When:  POST /api/cards { title: "Test", status: "TODO", assigneeId: user_B.id }
Then:  201, card.assignee.id = user_B.id
And:   user_B может GET /api/boards/{boardId} → 200
```

**TS-CR-03: Создание карточки с несуществующим assignee**

```
Given: user_A (ADMIN) аутентифицирован
When:  POST /api/cards { assigneeId: 99999 }
Then:  404 Not Found
```

**TS-CR-04: Обновление assignee**

```
Given: card_id имеет assignee = user_B
When:  PUT /api/cards/{card_id} { assigneeId: user_C.id }
Then:  200, card.assignee.id = user_C.id
And:   user_C видит board (GET → 200)
And:   user_B видит board, если есть другая карточка; 404, если это была последняя
```

**TS-CR-05: Снятие assignee**

```
Given: card_id имеет assignee = user_B
When:  PUT /api/cards/{card_id} { assigneeId: null }
Then:  200, card.assignee = null
```

### 4.3 Backend: Atomicity (P1)

**TS-AT-01: Транзакционная целостность**

```
Given: user_A (ADMIN), user_B
When:  POST /api/cards { assigneeId: user_B.id }
And:   сервер падает после INSERT card, до COMMIT
Then:  card не создана, user_B не имеет доступа к board
```

### 4.4 Backend: Edge Cases (P1-P2)

**TS-EC-01: ADMIN назначает себя assignee**

```
Given: user_A (ADMIN)
When:  POST /api/cards { assigneeId: user_A.id }
Then:  201, card.creator.id = card.assignee.id = user_A.id
```

**TS-EC-02: Снятие assignee, другая карточка остаётся**

```
Given: board_id имеет card_1 (assignee = user_B) и card_2 (assignee = user_B)
When:  PUT /api/cards/{card_1.id} { assigneeId: null }
Then:  200, user_B всё ещё видит board через card_2
```

**TS-EC-03: Move не меняет assignee**

```
Given: card_id имеет assignee = user_B
When:  PATCH /api/cards/{card_id}/move { targetStatus: "DONE", targetIndex: 0 }
Then:  200, card.assignee.id = user_B (без изменений)
```

### 4.5 Backend: User Endpoint (P2)

**TS-US-01: ADMIN получает список пользователей**

```
Given: user_A (ADMIN)
When:  GET /api/users
Then:  200, [{ id: ..., username: "..." }]
And:   response не содержит password, email, roles
```

**TS-US-02: Assignee не может получить список пользователей**

```
Given: user_B (assignee, not ADMIN)
When:  GET /api/users
Then:  403 Forbidden
```

### 4.6 Frontend: Role-Based UI (P0-P1)

**TS-FE-01: Assignee не видит create/edit/delete**

```
Given: user_B (assignee) открывает BoardPage
Then:  Кнопки "Create card", "Edit board", "Delete board", 
       иконки edit/delete на карточках отсутствуют
       Column settings недоступны
```

**TS-FE-02: ADMIN видит полный UI**

```
Given: user_A (ADMIN) открывает BoardPage
Then:  Все кнопки и формы управления присутствуют
```

**TS-FE-03: CardForm отображает assignee dropdown**

```
Given: user_A (ADMIN) открывает форму создания/редактирования карточки
Then:  Dropdown выбора assignee с данными из GET /api/users
       Поле creator отображается read-only
```

**TS-FE-04: Card отображает creator и assignee**

```
Given: карточка с creator = "admin", assignee = "worker"
When:  Карточка рендерится на доске
Then:  Виден username создателя (read-only)
       Аватар с инициалами "WO" (или первая буква username)
```

**TS-FE-05: Placeholder при null assignee**

```
Given: карточка без assignee
When:  Карточка рендерится на доске
Then:  Placeholder вместо аватара (пустой круг / иконка)
```

---

## 5. Regression Strategy

### 5.1 Affected Existing Tests

| Existing Test | Что может сломаться | Fix |
|---------------|---------------------|-----|
| `KanbanIntegrationTest` (create card) | NOT NULL creator_id → SQL Error | Добавить creator в fixture |
| `KanbanIntegrationTest` (update card) | DTO изменился — новые поля | Обновить assertions |
| `KanbanIntegrationTest` (list boards) | Фильтрация по author_id — assignee не рассматривался | Без изменений (assignee нет в базовой конфигурации) |
| `AuthIntegrationTest` | Без изменений | — |

### 5.2 Regression Test Suite

| Suite | Тесты | CI Gate |
|-------|-------|---------|
| Full `mvn test` | Все существующие + новые IT | Да |
| `KanbanRegressionIntegrationTest` | Smoke: create/read/update/delete/move без assignee | Да |
| `AuthRegressionIntegrationTest` | Login/logout/me/401 | Да |

---

## 6. NFR and Security Testing

| NFR | Тест-сценарий | Risk |
|-----|--------------|------|
| NFR-1 (Приватность) | assignee видит ВСЕ карточки доски (AC-6.2) | Medium |
| NFR-2 (Производительность) | Board с 1000 cards + EXISTS — < 500ms | Low (deferred) |
| NFR-4 (Атомарность) | @Transactional + pessimistic lock — нет race condition | High |
| NFR-5 (Идемпотентность) | Повторный assign того же пользователя — без ошибки | Low |
| Security (AD-3) | `@PreAuthorize("hasRole('ADMIN')")` на мутациях | Critical |
| Security (AD-13) | GET /api/users доступен только ADMIN | Critical |
| Security (AD-5) | Чужие board/card id → 404, не 403 | Medium |

---

## 7. Environment and Test Data Requirements

### 7.1 Preconditions

| # | Precondition | Статус |
|---|-------------|--------|
| P-1 | Минимум 2 пользователя в БД (ADMIN + Worker) | **BLOCKER** — требуется механизм создания пользователей |
| P-2 | Существующая board с карточками (с assignee и без) | Тестовые фикстуры |
| P-3 | Flyway V3 выполнена (поля creator_id, assignee_id) | Миграция |
| P-4 | FE собран с новыми компонентами | Build |

### 7.2 Тестовые пользователи

| Username | Role | Use |
|----------|------|-----|
| `test-admin` | ADMIN | Создание мутаций, управление |
| `test-worker` | — | Assignee (not ADMIN) |
| `test-observer` | — | Не-участник (не имеет доступа) |

---

## 8. Test Deliverables

| Артефакт | Описание | Owner |
|----------|----------|-------|
| `CardAssigneeIntegrationTest.java` | CT-1..CT-5, EC-1, EC-2, EC-8 | BE Dev |
| `BoardAccessIntegrationTest.java` | CT-6, CT-7, EC-3, EC-4 | BE Dev |
| `UserControllerIntegrationTest.java` | CT-8, TS-US-01, TS-US-02 | BE Dev |
| `CardMoveIntegrationTest.java` | CT-9 | BE Dev |
| `KanbanRegressionIntegrationTest.java` | CT-10 | BE Dev |
| `Avatar.test.tsx` (future) | TS-FE-05 | FE Dev |
| `CardForm.test.tsx` (future) | TS-FE-03 | FE Dev |
| `BoardPage.test.tsx` (future) | TS-FE-01, TS-FE-02 | FE Dev |

---

## 9. Open Questions (для create-epics-and-stories)

| OQ | Вопрос | Влияние на тестирование |
|----|--------|------------------------|
| OQ-TE-1 | Как создаются тестовые пользователи? SQL-фикстура или POST /api/users? | Тестовые данные; без решения — тесты assignee не запустить |
| OQ-TE-2 | FE Vitest-тесты — добавлять в этот epic или отдельной задачей? | Coverage FE BoardPage |
| OQ-TE-3 | E2E-тесты в autotests/ — писать параллельно или deferred? | CI pipeline |
| OQ-TE-4 | Нужны ли ручные тест-кейсы для assignee UI? | Если FE не покрыт Vitest |
| OQ-TE-5 | Какая стратегия для существующих карточек без creator в тестовых БД? | Fixture/data.sql |

---

## 10. Handoff Note для create-epics-and-stories

На основе System-Level Test Design определены следующие рекомендации для декомпозиции на epic:

1. **Epic 1 — Backend assignee/creator**: Card entity, Flyway V3, CardService, BoardService access, UserController
2. **Epic 2 — Backend Integration Tests**: CT-1..CT-10, покрытие P0-P1 сценариев
3. **Epic 3 — Frontend assignee UI**: Avatar, CardForm dropdown, Card display, BoardPage role adaptation
4. **Epic 4 — Precondition**: Создание пользователей (POST /api/users или SQL) — BLOCKER
5. **Epic 5 — Regression**: Обновление fixture, регрессионные тесты

**Приоритет тестирования:** Backend access control (P0) → Frontend role adaptation (P0) → Edge cases (P1) → FE components (P2)
