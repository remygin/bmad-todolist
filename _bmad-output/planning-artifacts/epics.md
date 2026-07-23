---
stepsCompleted:
  - 'step-01-validate-prerequisites'
  - 'step-02-design-epics'
  - 'step-03-create-stories'
  - 'step-04-final-validation'
inputDocuments:
  - '_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md'
  - '_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md'
  - '_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md'
  - '_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-23/architecture-delta.md'
  - '_bmad-output/test-artifacts/test-design/handoff.md'
  - '_bmad-output/project-context.md'
status: 'updated'
---

# bmad-todolist - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for bmad-todolist, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Неаутентифицированный посетитель может войти по корректным логину и паролю и получить Сессию (JWT); при неверных данных — 401; пароли только BCrypt; TTL по умолчанию 1 час (`JWT_EXPIRATION_MS`); при загрузке SPA недействительный токен удаляется, пользователь уходит на `/login`.
FR2: Администратор может запросить свой профиль и Роли по действующей Сессии; без/с недействительным токеном — 401.
FR3: Администратор может завершить работу, сбросив локальную Сессию (JWT из `sessionStorage` + очистка профиля); серверного revoke токена нет; после выхода защищённые экраны недоступны.
FR4: При старте система создаёт роль `ADMIN` и учётную запись Администратора из конфигурации, если их ещё нет; повторный старт не создаёт дубликатов.
FR5: Администратор может получить список своих Досок и открыть Доску с Колонками и Карточками; чужая Доска — 404; после загрузки списка первая Доска автоматически становится активной, если активная ещё не выбрана.
FR6: Администратор может создать Доску с именем до 120 символов; новая Доска сразу получает ровно три Колонки (`TODO`, `IN_PROGRESS`, `DONE`); имя уникально без учёта регистра в пределах Администратора (409 при конфликте); пустое/слишком длинное имя — 400.
FR7: Администратор может переименовать принадлежащую ему Доску с теми же правилами имени, что и при создании; чужая/несуществующая — 404.
FR8: Администратор может удалить принадлежащую ему Доску после подтверждения в UI; удаление каскадно удаляет Колонки и Карточки; чужая/несуществующая — 404.
FR9: Система гарантирует у любой Доски ровно три Колонки со Статусами `TODO`, `IN_PROGRESS`, `DONE`; набор без ровно трёх системных Статусов или с дубликатами отклоняется (400).
FR10: Администратор может атомарно сохранить полный набор из трёх Колонок с новыми отображаемыми именами и позициями (0..2); Статусы неизменны; имя после trim непустое и ≤80 символов (иначе 400).
FR11: Администратор может создать Карточку в выбранной Колонке с заголовком и необязательным описанием; карточка попадает в конец Колонки; пустой заголовок / title >200 / description >4000 — 400.
FR12: Администратор может изменить заголовок и описание Карточки с теми же правилами валидации; чужая/несуществующая — 404.
FR13: Администратор может удалить Карточку после подтверждения в UI; позиции оставшихся в Колонке нормализуются; чужая/несуществующая — 404.
FR14: Администратор может переместить Карточку (мышь/касание/клавиатура через drag-handle) по `targetStatus` + `targetIndex`; сервер атомарно применяет и нормализует Позиции; индекс вне диапазона — 400; UI оптимистически обновляет порядок и откатывает при ошибке; при успехе move UI не перечитывает Доску и не применяет ответ move (серверный порядок виден после следующей загрузки).
FR15: `GET /actuator/health` доступен без Сессии и возвращает HTTP 200 с `status: "UP"` при работоспособности; подробности внутренних компонентов не раскрываются.
FR-A1: `POST /api/cards` без `assigneeId` создаёт Карточку с `creator = текущий пользователь`, `assignee = null`. (AC-1.1)
FR-A2: `POST /api/cards` с валидным `assigneeId` создаёт Карточку с назначенным Исполнителем; Исполнитель получает доступ к Доске. (AC-1.2)
FR-A3: `POST /api/cards` с несуществующим `assigneeId` → 404. (AC-1.3)
FR-A4: `PUT /api/cards/{id}` с `assigneeId: null` снимает назначение; с новым `assigneeId` меняет Исполнителя. (AC-2.1, AC-2.2)
FR-A5: `GET /api/boards` возвращает Доски, где пользователь автор ИЛИ Исполнитель. (AC-3.3)
FR-A6: `GET /api/boards/{id}` для Исполнителя возвращает Доску (если есть хотя бы одна Карточка с его `assignee_id`); без доступа → 404. (AC-3.1, AC-3.2)
FR-A7: `POST /api/users` (только ADMIN) — создание нового пользователя системы. (BP-5 precondition)
FR-A8: `GET /api/users` (только ADMIN) — список пользователей (`id`, `username`) для выбора Исполнителя. (AC-5.4)

### NonFunctional Requirements

NFR1: Безопасность доступа — все Kanban-операции требуют роли `ADMIN` (403 без неё); аутентификация без `ADMIN` не даёт доступа к доскам и карточкам.
NFR2: Хранение секретов и паролей — пароли только BCrypt; секреты из окружения, не в коде; JWT только в `Authorization: Bearer` и `sessionStorage` (не localStorage/cookie/URL).
NFR3: Поверхность actuator — из служебных эндпоинтов извне доступен только health.
NFR4: Целостность данных — доменные инварианты (три Статуса, уникальность имени Доски per author, нормализация Позиций) обеспечиваются на сервере; при загрузке серверное состояние — источник истины.
NFR5: Согласованность контрактов — REST-API возвращает единый формат ошибок `{message, details}` с кодами 400/401/403/404/409; изменения `/api` синхронизируются между backend и frontend.
NFR6: Доступность взаимодействия — перемещение Карточек работает мышью, касанием и клавиатурой (drag-handle), а не только мышью.
NFR7: Наблюдаемость — доступен health-эндпоинт для мониторинга живости.
NFR8: Приватность развёртывания — данные и учётная запись живут внутри развёртывания владельца; внешних сервисов и передачи данных наружу нет.
NFR9: Граница безопасности — политика доступа, CSRF/session policy и bootstrap-Администратор — часть текущего контракта; чужие идентификаторы Досок/Карточек не раскрывают данные (404).
NFR10: Доменный инвариант как архитектурный контракт — ровно три системных Статуса; нарушение недопустимо (SM-C1).
NFR11: Конкурентность записи — мутации Досок и Карточек выполняются с проверкой владения и блокировкой записи (`findOwned*ForUpdate`), чтобы конкурентные операции не нарушали Позиции.
NFR-A1: Проверка доступа Исполнителя к Доске — через EXISTS-подзапрос к `cards.assignee_id` (без отдельной таблицы `board_members`).
NFR-A2: `creator_id` заполняется автоматически из `@AuthenticationPrincipal` (не user-supplied); `assignee_id` проверяется на существование (404).
NFR-A3: Назначение Исполнителя + выдача доступа к Доске — атомарно в одной `@Transactional`.
NFR-A4: Снятие Исполнителя проверяет EXISTS на другие Карточки той же Доски; доступ сохраняется, если остались назначения.
NFR-A5: `Card.creator` (NOT NULL) и `Card.assignee` (nullable) — LAZY + `@NamedEntityGraph("Card.withCreatorAndAssignee")`; загрузка внутри `@Transactional` (`open-in-view: false`).

### Additional Requirements

- Brownfield: starter/greenfield template не применяется; база кода уже существует (`backend/`, `frontend/`, Compose).
- AD-1: Сохранять слои BE Controller→Service→Repository и FE pages/components/api/auth; новое Kanban-поведение — в `board`/`card` и `components/kanban` + `api/kanban.ts`.
- AD-2: Единственный путь FE→BE — `/api` (JSON); backend не импортирует frontend; controllers вызывают только services.
- AD-3: Stateless JWT; CSRF disabled; `SessionCreationPolicy.STATELESS`; публичные `POST /api/auth/login` и `GET /actuator/health`; Kanban — `@PreAuthorize("hasRole('ADMIN')")`; ключ `todolist.accessToken`.
- AD-4: Идентичность колонки — `ColumnStatus`; `position` 0..2 — только порядок отображения; JSON display field — `name` (JPA `displayName`); FE/BE одинаковые литералы статусов.
- AD-5: Ownership по `author_id`; чужие/отсутствующие id → 404 (не 403).
- AD-6: Только `CardService` пишет Card rows; move payload `targetStatus`+`targetIndex`; `@dnd-kit` only; create/update/delete синхронизируют UI с успешным API; move — optimistic + rollback без re-apply ответа.
- AD-7: Схема только новым Flyway `V<next>__*.sql`; `ddl-auto=validate`; cascade delete Board→Columns/Cards через FK.
- AD-8: DTO — nested records в `*Dtos` + Bean Validation; `CardResponse` только в `CardDtos`; лимиты полей: board 120, column 80, title 200, description 4000.
- AD-9: Весь FE HTTP к `/api/**` через `apiRequest` в `client.ts`; board state — локальный React state (без global store).
- AD-10: Канонический deploy — Docker Compose (postgres:16-alpine → backend → frontend nginx); локально Vite proxy `/api`+`/actuator` → `localhost:8081`; Compose backend `:8080`; CORS `allowedOriginPatterns: *`, `allowCredentials: false` до явной security-задачи.
- AD-11: Board — aggregate root для ownership/locking; CardService — единственный writer Card; BoardService владеет Board/BoardColumn и собирает read model.
- AD-12: Canonical board detail — nested `columns[].cards[]`; list — summaries без cards; FE `Board` зеркалит дерево в `api/kanban.ts`.
- Тесты BE: касание `/api/**` → MockMvc IT с реальным login + Bearer; FE gate: `npm run lint` + `npm run build` (без Jest/Vitest/Playwright без задачи).
- UI copy и fallback ошибок — на русском.
- Deferred (только явной story): CI/CD; сужение CORS; server revoke / global 401 handler; sync FE state из успешного move response; multi-user/custom statuses/export — Non-Goals.
- AD-13: `GET /api/users` — ADMIN-only, `[{id, username}]`, без пагинации MVP; `?q=` зарезервирован.
- AD-14: `Card.creator` (NOT NULL) и `Card.assignee` (nullable) — LAZY + @EntityGraph; только CardService пишет assignee; creator из @AuthenticationPrincipal.
- AD-15: Удаление пользователя с `creator_id` запрещено; с `assignee_id` — SET NULL.
- IR-1..IR-11 (architecture-delta §5.1): assignee_id проверка существования; атомарность + доступ; идемпотентность повторного назначения; FE Avatar — инициалы; FE скрытие мутаций для assignee; move не меняет assignee.
- CT-1..CT-10 (architecture-delta §5.3): контрактные тесты backend: создание с/без assignee, доступ assignee, move без изменения assignee, регрессия.
- Precondition (OQ-5): механизм создания пользователей (`POST /api/users` или SQL) — BLOCKER для assignee-сценариев; без минимум 2 пользователей тестирование CT-1..CT-8 невозможно.
- TEA testability concern TC-1: BLOCKER — нет минимум 2 пользователей.
- TEA testability concern TC-2: HIGH — существующие IT сломаются при NOT NULL creator_id; fixture обновление обязательно.
- TEA testability concern TC-3: MEDIUM — H2 MODE=PostgreSQL и EXISTS; верифицировать поведение.
- TEA risks: R-TA-1 (assignee получает мутации — Critical), R-TA-2 (утечка данных — Critical), R-TA-6 (FE показывает мутации assignee — Critical).

### UX Design Requirements

UX-документа нет — UX Design Requirements не извлекались. Продуктовые UX-поведения, уже зафиксированные в PRD (auto-select первой доски, `window.confirm` перед удалением, purge invalid JWT на bootstrap, оптимистичный move), покрыты соответствующими FR.

### FR Coverage Map

FR1: Epic 1 — Вход по логину/паролю и получение JWT-Сессии
FR2: Epic 1 — Получение профиля текущего Администратора
FR3: Epic 1 — Выход и сброс локальной Сессии
FR4: Epic 1 — Bootstrap роли ADMIN и учётной записи
FR5: Epic 2 — Список и просмотр своих Досок
FR6: Epic 2 — Создание Доски с тремя системными Колонками
FR7: Epic 2 — Переименование Доски
FR8: Epic 2 — Удаление Доски (каскад)
FR9: Epic 2 — Инвариант ровно трёх системных Статусов
FR10: Epic 2 — Переименование и переупорядочивание Колонок
FR11: Epic 3 — Создание Карточки
FR12: Epic 3 — Редактирование Карточки
FR13: Epic 3 — Удаление Карточки
FR14: Epic 3 — Перемещение Карточки (DnD)
FR15: Epic 1 — Публичный health check
FR-A1: Epic 5 — Создание Карточки (creator + assignee)
FR-A2: Epic 5 — Создание Карточки с назначенным Исполнителем
FR-A3: Epic 5 — Создание Карточки с несуществующим assigneeId
FR-A4: Epic 5 — Редактирование Карточки (смена/снятие assignee)
FR-A5: Epic 5 — Список Досок для Исполнителя
FR-A6: Epic 5 — Просмотр Доски Исполнителем
FR-A7: Epic 4 — Создание пользователей (ADMIN-only)
FR-A8: Epic 4 — Список пользователей (ADMIN-only)

## Epic List

### Epic 1: Вход и сессия Администратора
Администратор входит по логину и паролю, работает в Сессии, видит свой профиль, выходит; система при старте создаёт роль ADMIN и учётную запись; эксплуатация проверяет живость сервиса через health.
**FRs covered:** FR1, FR2, FR3, FR4, FR15

### Epic 2: Управление досками и колонками
Администратор создаёт, просматривает, переименовывает и удаляет свои Доски; настраивает отображаемые имена и порядок трёх Колонок при жёстком инварианте TODO / IN_PROGRESS / DONE.
**FRs covered:** FR5, FR6, FR7, FR8, FR9, FR10

### Epic 3: Ведение карточек на доске
Администратор создаёт, редактирует, удаляет Карточки и перемещает их мышью, касанием или клавиатурой; сервер нормализует Позиции; UI применяет optimistic move с откатом при ошибке.
**FRs covered:** FR11, FR12, FR13, FR14

### Epic 4: Управление пользователями системы
Администратор создаёт новых пользователей и просматривает список всех пользователей системы. Обеспечивает precondition для назначения Исполнителя на Карточку.
**FRs covered:** FR-A7, FR-A8

### Epic 5: Назначение исполнителя и создателя на карточку
Каждая Карточка получает Создателя (автоматически) и опционального Исполнителя; Исполнитель может просматривать Доску и Карточки, но не мутировать их. Администратор управляет назначением через UI.
**FRs covered:** FR-A1, FR-A2, FR-A3, FR-A4, FR-A5, FR-A6

## Epic 1: Вход и сессия Администратора

Администратор входит по логину и паролю, работает в Сессии, видит свой профиль, выходит; система при старте создаёт роль ADMIN и учётную запись; эксплуатация проверяет живость сервиса через health.

### Story 1.1: Bootstrap роли ADMIN и учётной записи

As a оператор развёртывания,
I want чтобы при старте системы создавались роль `ADMIN` и учётная запись Администратора из конфигурации,
So that я мог войти без отдельной регистрации.

**Acceptance Criteria:**

**Given** чистая БД без пользователей и ролей
**When** приложение стартует с заданными `ADMIN_*` в окружении
**Then** существуют ровно одна роль `ADMIN` и ровно одна учётная запись Администратора
**And** пароль хранится только как BCrypt-хэш (не в открытом виде)

**Given** роль и Администратор уже существуют
**When** приложение стартует повторно
**Then** дубликаты роли и учётной записи не создаются

### Story 1.2: Вход по логину и паролю

As a Администратор,
I want войти по логину и паролю и получить Сессию (JWT),
So that я получил доступ к защищённым экранам и данным.

**Acceptance Criteria:**

**Given** bootstrap-Администратор существует
**When** посетитель отправляет корректные логин и пароль
**Then** возвращаются JWT и профиль пользователя с ролями
**And** по умолчанию токен действует 1 час (настраивается через `JWT_EXPIRATION_MS`)
**And** клиент хранит токен только в `sessionStorage` (`todolist.accessToken`)

**Given** указаны неверные учётные данные
**When** выполняется попытка входа
**Then** вход отклоняется с 401 и токен не выдаётся

**Given** в `sessionStorage` лежит недействительный или истёкший токен
**When** SPA загружается
**Then** токен удаляется из Сессии и пользователь перенаправляется на `/login`

### Story 1.3: Получение профиля текущего Администратора

As a Администратор,
I want запросить свой профиль и Роли по действующей Сессии,
So that UI знает, кто вошёл и какие у него права.

**Acceptance Criteria:**

**Given** валидный JWT в заголовке `Authorization: Bearer`
**When** запрашивается профиль текущего пользователя
**Then** возвращаются текущий пользователь и его роли

**Given** токен отсутствует или недействителен
**When** запрашивается профиль
**Then** запрос отклоняется с 401

### Story 1.4: Выход и сброс локальной Сессии

As a Администратор,
I want завершить работу с приложением, сбросив локальную Сессию,
So that защищённые экраны становятся недоступны без повторного входа.

**Acceptance Criteria:**

**Given** Администратор аутентифицирован
**When** выполняется выход
**Then** JWT удаляется из `sessionStorage` и локальный профиль очищается
**And** серверного revoke токена нет

**Given** выход выполнен
**When** пользователь пытается открыть защищённый маршрут
**Then** доступ без повторного входа невозможен (перенаправление на `/login`)

### Story 1.5: Публичный health check

As a оператор развёртывания,
I want публичную проверку живости сервиса,
So that мониторинг и прокси могли убедиться, что приложение работоспособно.

**Acceptance Criteria:**

**Given** приложение запущено и работоспособно
**When** выполняется `GET /actuator/health` без Сессии
**Then** возвращается HTTP 200 с `status: "UP"`
**And** подробности внутренних компонентов в публичном ответе не раскрываются

**Given** политика actuator из AD-10
**When** сервис развёрнут
**Then** из служебных эндпоинтов извне доступен только health

## Epic 2: Управление досками и колонками

Администратор создаёт, просматривает, переименовывает и удаляет свои Доски; настраивает отображаемые имена и порядок трёх Колонок при жёстком инварианте TODO / IN_PROGRESS / DONE.

### Story 2.1: Список и просмотр своих Досок

As a Администратор,
I want получить список своих Досок и открыть Доску с Колонками и Карточками,
So that я мог выбрать контекст работы и увидеть текущее состояние доски.

**Acceptance Criteria:**

**Given** Администратор аутентифицирован с ролью `ADMIN`
**When** запрашивается список Досок
**Then** возвращаются только Доски текущего Администратора (summaries без карточек)

**Given** список Досок загружен и активная Доска ещё не выбрана
**When** UI инициализирует выбор
**Then** первая Доска автоматически становится активной

**Given** Администратор открывает принадлежащую ему Доску
**When** запрашивается деталь Доски
**Then** возвращается nested read model: `columns[]` с `cards[]`, колонки упорядочены по `position`

**Given** указан id чужой или несуществующей Доски
**When** выполняется просмотр
**Then** возвращается 404 без раскрытия данных

### Story 2.2: Создание Доски с тремя Колонками

As a Администратор,
I want создать Доску с уникальным именем,
So that сразу получить контейнер с ровно тремя системными Колонками для работы.

**Acceptance Criteria:**

**Given** Администратор аутентифицирован
**When** создаётся Доска с валидным именем (после trim непустое, ≤120 символов)
**Then** возвращается Доска с ровно тремя Колонками со Статусами `TODO`, `IN_PROGRESS`, `DONE`
**And** имя уникально без учёта регистра в пределах Администратора

**Given** у Администратора уже есть Доска с таким же именем (без учёта регистра)
**When** выполняется создание
**Then** операция отклоняется с 409

**Given** имя пустое после trim или длиннее 120 символов
**When** выполняется создание
**Then** операция отклоняется с 400

### Story 2.3: Переименование Доски

As a Администратор,
I want переименовать принадлежащую мне Доску,
So that имя отражало актуальный контекст работы.

**Acceptance Criteria:**

**Given** Администратор владеет Доской
**When** отправляется новое валидное имя (непустое после trim, ≤120, уникальное без учёта регистра)
**Then** имя Доски обновляется и возвращается клиенту

**Given** новое имя конфликтует с другой Доской того же Администратора или нарушает правила длины/пустоты
**When** выполняется переименование
**Then** операция отклоняется с 409 (конфликт) или 400 (валидация)

**Given** Доска чужая или не существует
**When** выполняется переименование
**Then** возвращается 404

### Story 2.4: Удаление Доски

As a Администратор,
I want удалить принадлежащую мне Доску после подтверждения в UI,
So that ненужный контекст и связанные данные исчезли.

**Acceptance Criteria:**

**Given** Администратор владеет Доской с Колонками и Карточками
**When** удаление подтверждено в UI (`window.confirm`) и выполнен запрос удаления
**Then** Доска удаляется, а Колонки и Карточки удаляются каскадно (FK `ON DELETE CASCADE`)

**Given** Доска чужая или не существует
**When** выполняется удаление
**Then** возвращается 404

### Story 2.5: Настройка имён и порядка Колонок

As a Администратор,
I want атомарно сохранить полный набор из трёх Колонок с новыми отображаемыми именами и позициями,
So that Доска соответствовала моему языку процесса без изменения системных Статусов.

**Acceptance Criteria:**

**Given** Администратор владеет Доской с тремя системными Колонками
**When** сохраняется полный набор из трёх Колонок с новыми `name` и `position` (0..2)
**Then** отображаемые имена и позиции обновляются, Статусы остаются `TODO` / `IN_PROGRESS` / `DONE`
**And** сохранение выполняется как единая атомарная операция

**Given** набор Колонок не содержит ровно три системных Статуса или содержит дубликат Статуса
**When** выполняется сохранение
**Then** операция отклоняется с 400 (инвариант FR9)

**Given** отображаемое имя после trim пустое или длиннее 80 символов
**When** выполняется сохранение
**Then** операция отклоняется с 400

**Given** UI отображает Колонки
**When** Доска загружена
**Then** колонки рендерятся по `position`, а не по порядку объявления enum; JSON-поле имени — `name` (JPA `displayName`)

## Epic 3: Ведение карточек на доске

Администратор создаёт, редактирует, удаляет Карточки и перемещает их мышью, касанием или клавиатурой; сервер нормализует Позиции; UI применяет optimistic move с откатом при ошибке.

### Story 3.1: Создание Карточки

As a Администратор,
I want создать Карточку в выбранной Колонке Доски с заголовком и необязательным описанием,
So that новая единица работы появилась в нужной стадии.

**Acceptance Criteria:**

**Given** Администратор владеет Доской
**When** создаётся Карточка с валидным заголовком (после trim непустой, ≤200) и описанием ≤4000 в выбранном Статусе
**Then** Карточка создаётся с указанным Статусом и попадает в конец соответствующей Колонки
**And** UI применяет успешный ответ API в локальное состояние (или перезагружает Доску)
**And** сервер нормализует Позиции в Колонке

**Given** заголовок пустой после trim, длиннее 200 символов или описание длиннее 4000
**When** выполняется создание
**Then** операция отклоняется с 400

**Given** Доска чужая или не существует
**When** выполняется создание
**Then** возвращается 404

### Story 3.2: Редактирование Карточки

As a Администратор,
I want изменить заголовок и описание Карточки,
So that содержание задачи оставалось актуальным.

**Acceptance Criteria:**

**Given** Администратор владеет Карточкой
**When** отправляются валидные заголовок и описание
**Then** обновлённые поля сохраняются и возвращаются клиенту
**And** UI синхронизирует локальное состояние с успешным ответом API

**Given** заголовок пустой после trim, длиннее 200 символов или описание длиннее 4000
**When** выполняется обновление
**Then** операция отклоняется с 400

**Given** Карточка чужая или не существует
**When** выполняется обновление
**Then** возвращается 404

### Story 3.3: Удаление Карточки

As a Администратор,
I want удалить Карточку с Доски после подтверждения в UI,
So that ненужная задача исчезла, а порядок оставшихся остался непрерывным.

**Acceptance Criteria:**

**Given** Администратор владеет Карточкой в Колонке с несколькими Карточками
**When** удаление подтверждено в UI (`window.confirm`) и выполнен запрос удаления
**Then** Карточка удаляется, а Позиции оставшихся в Колонке нормализуются без «дыр»
**And** UI синхронизирует локальное состояние с результатом операции

**Given** Карточка чужая или не существует
**When** выполняется удаление
**Then** возвращается 404

### Story 3.4: Перемещение Карточки (drag-and-drop)

As a Администратор,
I want переместить Карточку в другую Колонку и на нужную Позицию мышью, касанием или клавиатурой через drag-handle,
So that визуально отразить прогресс работы с сохранением серверного порядка.

**Acceptance Criteria:**

**Given** Администратор владеет Карточкой на Доске
**When** выполняется перемещение с `targetStatus` и `targetIndex` (0..размер целевой Колонки после извлечения перемещаемой Карточки включительно)
**Then** сервер атомарно применяет перемещение и нормализует Позиции
**And** мутация выполняется с проверкой владения и `findOwned*ForUpdate` (pessimistic lock)

**Given** `targetIndex` вне допустимого диапазона
**When** выполняется перемещение
**Then** операция отклоняется с 400

**Given** UI поддерживает DnD через `@dnd-kit`
**When** Администратор перетаскивает Карточку мышью, касанием или клавиатурой (drag-handle)
**Then** UI немедленно применяет локальный оптимистический порядок
**And** при ошибке API восстанавливается предыдущее состояние
**And** при успехе UI не перечитывает Доску и не применяет возвращённый ответ move; серверный порядок виден после следующей загрузки Доски

**Given** Карточка чужая или не существует
**When** выполняется перемещение
**Then** возвращается 404

## Epic 4: Управление пользователями системы

Администратор создаёт новых пользователей и просматривает список всех пользователей системы для выбора Исполнителя. Без минимум 2 пользователей сценарии назначения Исполнителя не тестируемы.

### Story 4.1: Создание пользователя (slug: user-creation-endpoint)

As a Администратор,
I want создать нового пользователя системы,
So that он мог быть назначен Исполнителем на Карточки.

**Acceptance Criteria:**

**Given** Администратор аутентифицирован с ролью `ADMIN`
**When** отправляется `POST /api/users` с валидным `username` и `password`
**Then** создаётся новый пользователь; возвращается его `id` и `username`
**And** пароль сохраняется только как BCrypt-хэш

**Given** `username` пустой после trim или отсутствует
**When** выполняется `POST /api/users`
**Then** возвращается 400

**Given** `username` уже занят
**When** выполняется `POST /api/users` с тем же именем
**Then** возвращается 409

**Given** запрос без JWT или с истёкшим токеном
**When** выполняется `POST /api/users`
**Then** возвращается 401

**Given** запрос от пользователя без роли `ADMIN`
**When** выполняется `POST /api/users`
**Then** возвращается 403

**Given** попытка удалить пользователя с существующими `creator_id`
**When** выполняется удаление
**Then** операция запрещена (AD-15)

**Given** пользователь назначен Исполнителем на Карточки
**When** пользователь удаляется
**Then** `assignee_id` в его Карточках устанавливается в NULL

**Implementation notes:**
- Endpoint: `POST /api/users` — только ADMIN (OQ-5 resolution)
- DTO: `CreateUserRequest { username, password }` → `UserResponse { id, username }`
- Валидация: username после trim непустой, ≤120 символов; password не пустой
- При удалении пользователя: запрет если есть `creator_id`; SET NULL если есть `assignee_id` (AD-15)
- Precondition: без этой фичи assignee-сценарии не тестируемы (TEA TC-1 — BLOCKER)

### Story 4.2: Список пользователей (slug: user-list-endpoint)

As a Администратор,
I want получить список всех пользователей системы,
So что выбрать Исполнителя при создании или редактировании Карточки.

**Acceptance Criteria:**

**Given** Администратор аутентифицирован с ролью `ADMIN`
**When** выполняется `GET /api/users`
**Then** возвращается массив `[{id, username}]` всех пользователей системы
**And** пароль, email, роли не включены в ответ (NFR-A2)

**Given** в системе нет пользователей кроме Администратора
**When** выполняется `GET /api/users`
**Then** возвращается пустой массив `[]` (A-7)

**Given** запрос от пользователя без роли `ADMIN`
**When** выполняется `GET /api/users`
**Then** возвращается 403 (AD-13)

**Implementation notes:**
- `@PreAuthorize("hasRole('ADMIN')")` (AD-3, AD-13)
- Возвращает `UserDto { Long id, String username }` — без пароля, email, ролей
- Пагинация deferred для MVP; query param `?q=` заложен в интерфейсе сервиса
- UI: вызывается из `CardForm` для dropdown выбора Исполнителя (AC-5.4)

### Story 4.3: Тесты управления пользователями (slug: user-management-tests)

As a разработчик,
I want интеграционные тесты для user-эндпоинтов,
So that функциональность создания и списка пользователей проверена.

**Acceptance Criteria:**

**Given** `POST /api/users` реализован
**When** выполняется тест создания пользователя
**Then** проверяется: успешное создание (200), пустой username (400), дубликат (409), 401 без JWT, 403 для не-ADMIN

**Given** `GET /api/users` реализован
**When** выполняется тест списка пользователей
**Then** проверяется: возврат всех пользователей, пустой массив, 403 для не-ADMIN

**Given** тесты используют H2 MODE=PostgreSQL
**When** EXISTS-подзапрос выполняется в тесте
**Then** поведение EXISTS должно быть верифицировано; при проблемах — заменить на `COUNT > 0` (TEA TC-3)

**Implementation notes:**
- Тест-класс: `UserControllerIntegrationTest.java`
- Auth через реальный `POST /api/auth/login` + Bearer
- Проверка 401/403 для неаутентифицированных и не-ADMIN
- Fixture: `test/resources/data.sql` с multi-user данными

## Epic 5: Назначение исполнителя и создателя на карточку

Каждая Карточка получает Создателя (фиксируется при создании) и опционального Исполнителя. Исполнитель получает доступ ко всей Доске (может просматривать все Карточки). Администратор управляет назначением через UI. Move-операция не меняет assignee.

### Story 5.1: Миграция БД и модель Card (slug: card-entity-migration)

As a разработчик,
I want добавить поля `creator_id` и `assignee_id` в таблицу `cards` и обновить entity Card,
So что Карточка может хранить Создателя и Исполнителя.

**Acceptance Criteria:**

**Given** БД содержит существующие Карточки
**When** применяется Flyway `V3__add_card_users.sql`
**Then** в таблицу `cards` добавляются колонки `creator_id BIGINT NOT NULL` и `assignee_id BIGINT NULL`
**And** все существующие Карточки получают `creator_id = id bootstrap-администратора` (бэкфилл)
**And** `assignee_id` = NULL для всех существующих Карточек
**And** создаются индексы `idx_cards_creator_id` и `idx_cards_assignee_id`

**Given** `Card.java` entity обновлён
**When** выполняется загрузка Карточки
**Then** `creator` — `@ManyToOne(fetch = LAZY) @JoinColumn(nullable = false)`, `assignee` — `@ManyToOne(fetch = LAZY) @JoinColumn(nullable = true)`
**And** `@NamedEntityGraph("Card.withCreatorAndAssignee")` определён с `creator` и `assignee`

**Given** `CardRepository` обновлён
**When** вызывается `existsByBoardIdAndAssigneeId(boardId, userId)`
**Then** возвращается boolean — есть ли на Доске хотя бы одна Карточка с данным Исполнителем

**Given** миграция выполнена
**When** `spring.jpa.hibernate.ddl-auto=validate` проверяет схему
**Then** валидация проходит (схема соответствует entity)

**Implementation notes:**
- Flyaway: ALTER TABLE → бэкфилл → SET NOT NULL → FK → индексы (architecture-delta §4.5)
- `ddl-auto=validate` — схема только через Flyway (AD-7)
- Тестовые H2 данные нужно обновить (creator_id обязателен) — Story 5.7
- R-TA-3 (High): NOT NULL creator_id ломает существующие тесты без fixture

### Story 5.2: CardService — логика assignee (slug: card-service-assignee)

As a разработчик,
I want реализовать логику создания и обновления Карточки с creator/assignee,
So что Карточка корректно фиксирует Создателя и опционального Исполнителя.

**Acceptance Criteria:**

**Given** Администратор создаёт Карточку без `assigneeId`
**When** вызывается `CardService.create()`
**Then** `creator = текущий пользователь` (из `@AuthenticationPrincipal`), `assignee = null` (AC-1.1)

**Given** Администратор создаёт Карточку с валидным `assigneeId`
**When** вызывается `CardService.create()`
**Then** `creator = текущий пользователь`, `assignee = найденный пользователь`, Исполнитель получает доступ к Доске (AC-1.2)
**And** создание + выдача доступа выполняются в одной `@Transactional` (NFR-A3)

**Given** `assigneeId` указывает на несуществующего пользователя
**When** вызывается `CardService.create()`
**Then** возвращается 404 (AC-1.3, IR-2)

**Given** Администратор обновляет Карточку с `assigneeId: null`
**When** вызывается `CardService.update()`
**Then** назначение снимается (AC-2.1)
**And** проверяется EXISTS по другим Карточкам той же Доски: если остались назначения — доступ сохраняется; если это была последняя — доступ теряется (EC-2, IR-4)

**Given** Администратор обновляет Карточку с новым `assigneeId`
**When** вызывается `CardService.update()`
**Then** Исполнитель меняется; новый Исполнитель получает доступ к Доске (AC-2.2)

**Given** Администратор назначает того же Исполнителя повторно
**When** вызывается `CardService.update()`
**Then** операция идемпотентна (BP-10, EC-8, IR-11)

**Given** Карточка перемещается (drag-and-drop)
**When** вызывается `CardService.move()`
**Then** `assignee` не меняется — move payload не включает `assigneeId` (OQ-10, IR-10)

**Given** Администратор назначает себя Исполнителем
**When** создаётся Карточка с `assigneeId = self`
**Then** `creator = assignee = ADMIN` (EC-1)

**Given** Карточка чужая или не существует
**When** выполняется create/update/move
**Then** возвращается 404 (AD-5)

**Implementation notes:**
- `creator_id` заполняется автоматически из `@AuthenticationPrincipal`, не из payload (IR-1)
- `assignee_id` проверяется на существование (IR-2)
- Атомарность: назначение + доступ в одной `@Transactional` (IR-3)
- При снятии последнего assignee — доступ теряется немедленно (OQ-2 resolution)
- Мутации остаются `@PreAuthorize("hasRole('ADMIN')")` (NFR1, AD-3)
- `CardResponse` обновлён: `creatorId`, `creatorUsername`, `assigneeId`, `assigneeUsername`

### Story 5.3: BoardService — доступ для Исполнителя (slug: board-access-extension)

As a разработчик,
I want расширить проверку доступа к Доске: автор ИЛИ Исполнитель,
So что Исполнитель может просматривать Доски, где есть его Карточки.

**Acceptance Criteria:**

**Given** пользователь — Исполнитель на хотя бы одной Карточке Доски
**When** выполняется `GET /api/boards/{id}`
**Then** Доска возвращается (200) со всеми Колонками и Карточками (AC-3.1, OQ-1)

**Given** пользователь не является автором и не имеет Карточек на Доске
**When** выполняется `GET /api/boards/{id}`
**Then** возвращается 404 (AC-3.2, AD-5)

**Given** пользователь — Исполнитель на Доске
**When** выполняется `GET /api/boards`
**Then** Доска включена в список (AC-3.3)

**Given** проверка доступа
**When** выполняется любой GET-запрос к Доске
**Then** логика: `author_id = ? OR EXISTS(SELECT 1 FROM cards WHERE board_id = ? AND assignee_id = ?)` (AC-4.1, NFR-A1)

**Given** Исполнитель пытается мутировать Доску или Карточки
**When** выполняется POST/PUT/DELETE
**Then** `@PreAuthorize("hasRole('ADMIN')")` отклоняет запрос (403) (R-TA-1)

**Given** чужая Доска (не автор и не исполнитель)
**When** выполняется GET
**Then** 404, данные не раскрываются (R-TA-2)

**Implementation notes:**
- Новая логика: `BoardService.canAccessBoard(userId, boardId)` — EXISTS-подзапрос
- `BoardRepository.findByAuthorIdOrAssigneeId(userId)` для списка Досок
- `@PreAuthorize("hasRole('ADMIN')")` сохраняется на всех мутациях
- EXISTS-подзапрос без кэширования для MVP (NFR-A1); deferred кэш при >1000 карточек
- H2 EXISTS — верифицировать поведение (TEA TC-3)

### Story 5.4: FE — Avatar component и отображение creator/assignee (slug: card-assignee-display)

As a Администратор и Исполнитель,
I want видеть на Карточке Создателя (username) и Исполнителя (аватар с инициалами),
So что понятно, кто создал задачу и на кого она назначена.

**Acceptance Criteria:**

**Given** Карточка имеет Создателя
**When** Карточка отображается
**Then** виден username Создателя (read-only) (AC-5.1)

**Given** Карточка имеет Исполнителя
**When** Карточка отображается
**Then** виден аватар с инициалами Исполнителя (первые 1-2 буквы username) (AC-5.2, C-8)

**Given** Карточка не имеет Исполнителя (`assignee = null`)
**When** Карточка отображается
**Then** вместо аватара показывается placeholder (AC-5.2)
**And** FE проверяет `assignee` на null перед рендерингом (RGR-6)

**Given** Avatar компонент
**When** рендерится
**Then** это круг с серым фоном (`#CBD5E1`), размер small (24px) или medium (32px), внутри инициалы

**Implementation notes:**
- Новый компонент: `components/kanban/Avatar.tsx` — круг с инициалами
- Размеры: small (24px) для карточки, medium (32px) для будущих расширений
- Без gravatar/загрузки фото (C-8)
- Цвет фона: `#CBD5E1` для MVP
- Обновление `api/kanban.ts`: типы Card с `creatorId`, `creatorUsername`, `assigneeId`, `assigneeUsername`
- `CardResponse` FE тип содержит все поля creator/assignee

### Story 5.5: FE — форма создания/редактирования и адаптация ролей (slug: assignee-ui-features)

As a Администратор,
I want выбирать Исполнителя при создании или редактировании Карточки,
So что назначать задачи на других пользователей.

**Acceptance Criteria:**

**Given** Администратор открывает форму создания/редактирования Карточки
**When** форма отображается
**Then** присутствует dropdown выбора Исполнителя (AC-5.3)
**And** dropdown загружает список пользователей через `GET /api/users` (AC-5.4)
**And** Creator отображается read-only

**Given** Администратор выбирает Исполнителя из dropdown
**When** форма сохраняется
**Then** `assigneeId` включён в payload; Исполнитель получает доступ к Доске

**Given** пользователь — Исполнитель (не ADMIN)
**When** открывается Dashboard
**Then** видны Доски, где у пользователя есть хотя бы одна Карточка (AC-6.1)
**And** Доска отображается целиком (все Карточки, включая чужие) (AC-6.2, OQ-1)

**Given** пользователь — Исполнитель
**When** открывается Доска
**Then** НЕ видны кнопки/формы создания, редактирования, удаления Карточек (AC-6.3, R-TA-6)
**And** НЕ виден UI управления Доской/колонками (AC-6.4)

**Given** пользователь — Администратор
**When** открывается Доска
**Then** полный UI: создание, редактирование, удаление, настройка колонок

**Implementation notes:**
- `BoardPage.tsx` — условный рендеринг: `isAdmin = user.roles.includes('ADMIN')` (OQ-8)
- Новый модуль: `api/user.ts` — `getUsers(): Promise<UserDto[]>`
- `CardForm.tsx` — dropdown assignee (загрузка из getUsers); creator read-only
- FE без глобального store (C-5); состояние доступа — локальное
- UI-тексты на русском, в тоне существующих экранов

### Story 5.6: FE — types, API module и интеграционные тесты (slug: assignee-tests-regression)

As a разработчик,
I want обновить FE типы, API-модуль и интеграционные тесты для assignee-фичи,
So что полный цикл "backend + frontend" покрыт тестами, а существующие тесты не сломаны.

**Acceptance Criteria:**

**Given** `api/kanban.ts` обновлён
**When** типы `Card`, `CreateCardRequest`, `UpdateCardRequest` содержат `creatorId`, `creatorUsername`, `assigneeId`, `assigneeUsername`
**Then** FE компилируется без ошибок типов

**Given** `api/user.ts` создан
**When** вызывается `getUsers()`
**Then** возвращается `Promise<UserDto[]>` через `apiRequest`

**Given** integration tests для backend
**When** выполняется CT-1..CT-9 (architecture-delta §5.3)
**Then** все тесты проходят:
- CT-1: POST /api/cards без assigneeId — creator = current user, assignee = null
- CT-2: POST /api/cards с assigneeId — creator + assignee + assignee получает доступ
- CT-3: POST /api/cards с несуществующим assigneeId — 404
- CT-4: PUT /api/cards/{id} с assigneeId: null — снятие назначения
- CT-5: PUT /api/cards/{id} с новым assigneeId — смена + доступ новому
- CT-6: GET /api/boards — assignee видит доски где есть его карточки
- CT-7: GET /api/boards/{id} — assignee 200, чужой 404
- CT-8: GET /api/users — ADMIN 200, assignee 403
- CT-9: Move card — assignee не меняется
- CT-10: Regression — существующие тесты проходят

**Given** существующие Integration Tests
**When** выполняется тестовый набор после фичи
**Then** все существующие тесты проходят (RGR-4, R-TA-3)
**And** fixture `test/resources/data.sql` обновлён: все Карточки имеют `creator_id`

**Implementation notes:**
- FE gate: `npm run lint` + `npm run build` (без Vitest/Jest — deferred, TEA TC-4)
- Backend IT: MockMvc с реальным login + Bearer; H2 MODE=PostgreSQL
- Новые тест-классы: `CardAssigneeIntegrationTest`, `BoardAccessIntegrationTest`, `UserControllerIntegrationTest`, `CardMoveIntegrationTest`
- Fixture: обновить существующие тестовые данные (creator_id обязателен)
- E2E-тесты (autotests/) deferred — не включены в CI (TEA TC-5)
