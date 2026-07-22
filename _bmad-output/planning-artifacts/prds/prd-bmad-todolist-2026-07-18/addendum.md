---
title: BMAD Todo List — Параметры пользователей на карточках
parent: _bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md
parent_title: BMAD Todo List — Kanban с JWT-админом
parent_status: final
status: draft
created: 2026-07-22
updated: 2026-07-22
source: feature-intent, analysis-recommendations, product-impact-assessment, prd-format-decision
---

# PRD Addendum: Параметры пользователей на карточках (creator + assignee)

## 0. Назначение документа

Этот аддендум фиксирует дельту изменений к базовому PRD (`prd.md`, status: final от 2026-07-18) для feature «Параметры пользователей на карточках». Документ самодостаточен для согласования scope: описывает недостающие требования, acceptance criteria, ограничения, out of scope, затронутые области и открытые технические вопросы. Проектирование решения (architecture delta) — отдельный downstream-шаг.

**Формат:** PRD Addendum (`on_addendum`). Базовый PRD не редактируется inline; изменения к FR-11, FR-12 и Non-Goals §5 зафиксированы ниже как дельта.

---

## 1. Изменения в глоссарии PRD

Добавить в §3 Глоссарий следующие термины:

- **Creator (Создатель)** — пользователь, создавший Карточку. Устанавливается автоматически из `@AuthenticationPrincipal` при создании, NOT NULL. Не может быть изменён после создания.
- **Assignee (Исполнитель)** — пользователь, на которого назначена Карточка. Опциональный параметр, nullable. Устанавливается или сбрасывается через `assigneeId` в create/update.

---

## 2. Дельта функциональных требований

### 2.1 FR-11: Создание карточки — расширение

Базовое описание (PRD строка 182): «Администратор может создать Карточку в выбранной Колонке Доски с заголовком и необязательным описанием.»

**Дополнение:** Администратор может указать опционального Исполнителя (`assigneeId`) при создании Карточки. Создатель (`creator`) заполняется автоматически из сессии. В ответе возвращаются идентификаторы и имена Создателя и Исполнителя.

**Изменения во входных/выходных контрактах:**
- `CreateCardRequest`: добавлено опциональное поле `assigneeId` (`Long`, nullable).
- `CardResponse`: добавлены поля `creatorId` (`Long`), `creatorUsername` (`String`), `assigneeId` (`Long`, nullable), `assigneeUsername` (`String`, nullable).

**Изменения в Consequences (testable):**
- Карточка создаётся с `creatorId` = ID текущего аутентифицированного ADMIN, `creatorUsername` = username текущего ADMIN.
- Если `assigneeId` передан и валиден (существующий ADMIN): Карточка создаётся с указанным Исполнителем; в ответе заполнены `assigneeId` / `assigneeUsername`.
- Если `assigneeId` не передан или null: `assignee` = null.
- Если `assigneeId` указывает на несуществующего пользователя или пользователя без роли ADMIN: запрос отклоняется (400).
- Если `assigneeId` = 0 или отрицательное число: запрос отклоняется (400).

### 2.2 FR-12: Редактирование карточки — расширение

Базовое описание (PRD строка 190): «Администратор может изменить заголовок и описание Карточки.»

**Дополнение:** Администратор может изменить или сбросить Исполнителя Карточки. Изменение Исполнителя выполняется через тот же endpoint (`PUT /cards/{id}`) расширенным `UpdateCardRequest`.

**Изменения во входных контрактах:**
- `UpdateCardRequest`: добавлено опциональное поле `assigneeId` (`Long`, nullable).

**Поведение:**
- Если `assigneeId` передан (не null): Исполнитель обновляется на указанного пользователя (с валидацией существования и роли ADMIN).
- Если `assigneeId` = null: Исполнитель сбрасывается (assignee = null).
- Если поле `assigneeId` отсутствует в запросе: Исполнитель не меняется.
- Валидация assigneeId: те же правила, что при создании (существующий User + роль ADMIN, иначе 400).
- Изменение assignee не влияет на остальные поля карточки (title, description, status, position не меняются).

**Изменения в Consequences (testable):**
- Обновление assignee через `PUT /cards/{id}` сохраняется и возвращается в ответе.
- Сброс assignee (явный null) возвращает `assigneeId` = null, `assigneeUsername` = null.
- Несуществующий `assigneeId` или пользователь без ADMIN отклоняется (400).

### 2.3 Изменения в Non-Goals §5

Удалить из §5 PRD следующий пункт:

> «Нет вложений, комментариев, меток, сроков, напоминаний, **назначения исполнителей на Карточках** — только заголовок, описание, Статус, Позиция.»

Изменить на:

> «Нет вложений, комментариев, меток, сроков, напоминаний — только заголовок, описание, Статус, Позиция, Создатель, Исполнитель.»

---

## 3. Бизнес-правила

| ID | Правило | Комментарий |
|----|---------|-------------|
| BR-1 | `creator` — NOT NULL, устанавливается автоматически из `@AuthenticationPrincipal` при создании карточки | Не может быть передан клиентом |
| BR-2 | `assignee` — nullable, устанавливается/сбрасывается через `assigneeId` в create/update | Опциональный параметр |
| BR-3 | Если `assigneeId` передан, пользователь должен существовать и иметь роль `ADMIN` | Иначе 400 |
| BR-4 | Если `assigneeId` не передан или null — assignee = null | Явный сброс |
| BR-5 | assignee не влияет на права доступа к карточке | Доступ по-прежнему через Board.author |
| BR-6 | При миграции: creator существующих карточек = Board.author | Backfill через SQL UPDATE в V3 |
| BR-7 | Текущая версия — single-user (один ADMIN). Создатель всегда текущий admin | Упрощение для v1 |
| BR-8 | creator не может быть изменён после создания карточки | Инвариант |

---

## 4. Acceptance Criteria

### AC-1: Создание карточки с creator
- **Given** аутентифицированный ADMIN
- **When** создаётся карточка без assigneeId
- **Then** в ответе `creatorId` = ID текущего пользователя, `creatorUsername` = username текущего пользователя, `assigneeId` = null, `assigneeUsername` = null

### AC-2: Создание карточки с assignee
- **Given** аутентифицированный ADMIN
- **When** создаётся карточка с валидным `assigneeId` существующего ADMIN
- **Then** в ответе `assigneeId` и `assigneeUsername` соответствуют указанному пользователю

### AC-3: Создание карточки с несуществующим assigneeId
- **Given** аутентифицированный ADMIN
- **When** создаётся карточка с `assigneeId` несуществующего пользователя
- **Then** ответ 400

### AC-4: Создание карточки с assigneeId не-ADMIN
- **Given** аутентифицированный ADMIN
- **When** создаётся карточка с `assigneeId` пользователя без роли ADMIN
- **Then** ответ 400

### AC-5: Обновление assignee через edit
- **Given** существующая карточка
- **When** отправляется PUT /cards/{id} с валидным `assigneeId`
- **Then** assignee обновляется, остальные поля не меняются

### AC-6: Сброс assignee через edit
- **Given** карточка с установленным assignee
- **When** отправляется PUT /cards/{id} с `assigneeId` = null
- **Then** assignee сбрасывается в null

### AC-7: Миграция существующих карточек (Flyway V3)
- **Given** БД с карточками, созданными до V3
- **When** применяется Flyway V3
- **Then** все существующие карточки получают `creator_id` = `board.author_id`

### AC-8: Отображение на фронтенде
- **Given** карточка с creator и assignee
- **When** карточка рендерится
- **Then** отображаются `creatorUsername` и (если есть) `assigneeUsername`

---

## 5. Edge Cases

| ID | Сценарий | Ожидаемое поведение |
|----|----------|---------------------|
| EC-1 | `assigneeId` = 0 или отрицательное число | 400 (ошибка валидации) |
| EC-2 | `assigneeId` = ID текущего пользователя (creator = assignee) | Допустимо. Карточка назначена на самого создателя |
| EC-3 | Обновление карточки: title + assignee одновременно | Допустимо. Оба поля обновляются в одном запросе |
| EC-4 | `assigneeId` указывает на ADMIN с другой доски | Допустимо — assignee может быть любым ADMIN в системе |
| EC-5 | Удаление пользователя, который является assignee | В single-user v1 неактуально. В будущем — потребуется политика |
| EC-6 | `assigneeId` передан в moveCard | move не включает assignee. assignee не меняется при перемещении |
| EC-7 | Две карточки с одинаковым assignee | Допустимо. Нет ограничения уникальности |
| EC-8 | `assigneeId` = null в UpdateCardRequest vs отсутствие поля | Явный null → сброс assignee. Отсутствие поля → assignee не меняется |

---

## 6. Ограничения

| ID | Ограничение | Обоснование |
|----|-------------|-------------|
| L-1 | Только роль ADMIN может быть assignee | Валидация в CardService |
| L-2 | assignee не получает прав на карточку | Доступ через Board.author |
| L-3 | При single-user: assignee может быть только тот же admin или null | Поле на фронтенде может быть скрыто или упрощено |
| L-4 | Нет отдельного эндпоинта для смены assignee | assignee меняется через общий PUT /cards/{id} |
| L-5 | Не добавлять страницу управления пользователями | Non-Goal |
| L-6 | creator не изменяется после создания | Инвариант домена |
| L-7 | assignee не меняется при перемещении карточки (moveCard) | Не входит в scope перемещения |

---

## 7. Out of Scope (явно)

- Добавление страницы управления пользователями, регистрации, смены пароля.
- Новые роли (только ADMIN).
- Изменение Board / BoardResponse / BoardSummary.
- Изменение эндпоинтов boards, columns, auth.
- Фильтрация или сортировка карточек по assignee на фронтенде.
- Отдельный UI-экран — только расширение существующих компонентов.
- Уведомления при назначении assignee.
- История изменений assignee (audit log).
- Каскадное поведение при удалении User (в single-user v1 неактуально).
- Смена creator после создания карточки.

---

## 8. Затронутые области (Affected Areas)

### Backend

| Компонент | Путь | Изменения |
|-----------|------|-----------|
| Card entity | `backend/.../card/Card.java` | + поля `creator` (ManyToOne User, NOT NULL), `assignee` (ManyToOne User, nullable) |
| Card DTOs | `backend/.../card/CardDtos.java` | + `creatorId`, `creatorUsername`, `assigneeId`, `assigneeUsername` в CardResponse; + `assigneeId` в CreateCardRequest, UpdateCardRequest |
| Card Service | `backend/.../card/CardService.java` | + установка creator при create из principal; + обработка assignee при create/update; + валидация assigneeId (существование User + роль ADMIN) |
| Card Controller | `backend/.../card/CardController.java` | Без изменений или минимальные (assignee через PUT /cards/{id}) |
| Card Repository | `backend/.../card/CardRepository.java` | Без изменений (новые queries опционально) |
| Board Service | `backend/.../board/BoardService.java` | `toCardResponse` — синхронизировать конструктор CardResponse |
| User Repository | `backend/.../user/UserRepository.java` | `findById` уже есть; метод поиска по роли — опционально |
| Flyway | `backend/src/main/resources/db/migration/V3__add_card_users.sql` | Новый файл миграции |
| Интеграционные тесты | `backend/src/test/.../kanban/KanbanIntegrationTest.java` | + тесты creator/assignee |

### Frontend

| Компонент | Путь | Изменения |
|-----------|------|-----------|
| API types | `frontend/src/api/kanban.ts` | + поля в Card; + assigneeId в createCard/updateCard |
| KanbanCard | `frontend/src/components/kanban/KanbanCard.tsx` | + отображение creatorUsername / assigneeUsername |
| CardForm | `frontend/src/components/kanban/CardForm.tsx` | + опциональное поле выбора assignee |

### База данных

| Объект | Изменения |
|--------|-----------|
| `cards` table | + `creator_id` (BIGINT, NOT NULL, FK → users(id)); + `assignee_id` (BIGINT, nullable, FK → users(id)) |
| Индексы | Опционально: индексы на `creator_id`, `assignee_id` |

### Документация

| Документ | Изменения |
|-----------|-----------|
| `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md` | Дельта зафиксирована в addendum (FR-11, FR-12, Non-Goals §5, Глоссарий) |
| `_bmad-output/project-context.md` | + правила о creator/assignee |

### Новый компонент (опционально)

| Компонент | Описание |
|-----------|----------|
| `GET /api/users` | Новый эндпоинт для получения списка ADMIN (id + username) для UI выбора assignee |

---

## 9. Открытые технические вопросы

Вопросы оформлены по шаблону BMAD:

---

### OQ-1: Сборка CardResponse в двух местах

**Статус:** открыт

**Контекст:** `CardResponse` собирается в `CardService.toResponse` и в `BoardService.toCardResponse`. При добавлении 4 новых полей необходимо синхронизировать оба места.

**Варианты:**
- A. Вынести общий builder/mapper в отдельный класс/метод.
- B. BoardService делегирует CardService для сборки CardResponse.
- C. Дублировать поля в обоих местах (с риском рассинхронизации).

**Рекомендация:** вариант A (вынести общий mapper). Решение за architecture delta.

---

### OQ-2: Новый эндпоинт GET /api/users

**Статус:** открыт

**Контекст:** для выбора assignee на фронтенде нужен список пользователей.

**Варианты:**
- A. Добавить `GET /api/users`, возвращающий `[{id, username}]` всех ADMIN.
- B. При single-user скрыть поле assignee (всегда null или текущий admin).
- C. Возвращать список пользователей через существующий эндпоинт профиля.

**Рекомендация:** вариант A, если в будущем планируется multi-user. Решение за architecture delta.

---

### OQ-3: HTTP-статус для некорректного assigneeId

**Статус:** открыт

**Контекст:** единообразие с существующими ошибками.

**Варианты:**
- A. 400 Bad Request (бизнес-ошибка валидации).
- B. 404 Not Found (ресурс User не найден).
- C. 422 Unprocessable Entity.

**Рекомендация:** вариант A (400), в соответствии с существующей моделью `BadRequestException`. Решение за architecture delta.

---

### OQ-4: Тип UpdateCardRequest.assigneeId — nullable vs Optional

**Статус:** открыт

**Контекст:** необходимо различать «поле не передано» (не менять assignee) и «явный null» (сбросить assignee).

**Варианты:**
- A. `Long assigneeId` (nullable) — если поле отсутствует в JSON, Jackson оставляет null; невозможно отличить от явного null.
- B. `Optional<Long> assigneeId` — явно различает отсутствие и null (но Optional в DTO спорно).
- C. Отдельный булевый флаг `resetAssignee: boolean` + `assigneeId: Long` (nullable).
- D. Использовать `@JsonInclude(NON_NULL)` и семантику «если поле не в JSON — не менять; если null — сбросить».

**Рекомендация:** вариант D (наиболее идиоматичный для Spring). Требует настройки десериализации. Решение за architecture delta.

---

### OQ-5: Индексы на creator_id / assignee_id

**Статус:** открыт

**Контекст:** в V3 создаются FK на users(id). Индексы опциональны и нужны для будущей фильтрации.

**Варианты:**
- A. Добавить индексы в V3 сразу.
- B. Отложить до появления запросов с фильтрацией по creator/assignee.

**Рекомендация:** вариант A (индексы не вредят, дешёвая операция на текущем объёме). Решение за architecture delta.

---

### OQ-6: Валидация assigneeId — только существование User + ADMIN, или также права на доску?

**Статус:** открыт

**Контекст:** при двух ADMIN с разными досками assignee может быть с чужой доски.

**Варианты:**
- A. Любой ADMIN в системе может быть assignee (без дополнительной проверки прав).
- B. Проверять, что assignee — ADMIN, имеющий доступ к доске (осложняет модель).
- C. Проверять, что assignee — ADMIN, и это не author = assignee другой доски.

**Рекомендация:** вариант A (минимальная сложность, согласуется с BR-5). Решение за architecture delta.

---

## 10. Предположения (Assumptions)

- `User` entity и репозиторий уже существуют (реализованы в V1/V2).
- `CardService` имеет доступ к `UserRepository` через DI.
- `Board.author` используется для backfill creator_id существующих карточек (NOT NULL гарантирован).
- Board.author не может быть null (ограничение существующей модели).
- Карточки удаляются каскадом вместе с доской — FK на users не блокируют удаление Board.
- В системе не больше одного ADMIN (single-user v1).
- Pessimistic lock на Board (AD-5 из архитектуры) покрывает конкурентные обновления assignee.
- `@ManyToOne(fetch = FetchType.LAZY)` для creator/assignee — достаточен; загрузка в рамках @Transactional.
- Существующие тесты card CRUD не сломаются от появления новых полей в CardResponse (нестрогое сравнение).

---

## 11. Регрессионные риски

| ID | Риск | Причина | Вероятность | Митигация |
|----|------|---------|-------------|-----------|
| RR-1 | BoardService.getBoard возвращает карточки без creator/assignee | `BoardService.toCardResponse` дублирует конструктор CardResponse | Высокая | Синхронизировать оба места (см. OQ-1) |
| RR-2 | moveCard возвращает CardResponse без creator/assignee | move возвращает CardResponse | Средняя | Проверить, что moveCard использует общий конструктор |
| RR-3 | updateCard на фронтенде не передаёт assigneeId | Если assigneeId ожидается, но не передан | Средняя | Проверить обратную совместимость API |
| RR-4 | Drag-and-drop optimistic update расходится с серверным ответом | Существующее поведение (см. Note к FR-14) | Низкая | Не меняется в этом scope |
