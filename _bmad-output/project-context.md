---
project_name: 'bmad-todolist'
user_name: 'remygin'
date: '2026-07-22'
sections_completed:
  [
    'technology_stack',
    'language_rules',
    'framework_rules',
    'testing_rules',
    'quality_rules',
    'workflow_rules',
    'anti_patterns',
  ]
status: 'complete'
rule_count: 50
optimized_for_llm: true
existing_patterns_found: 12
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

- Backend: Java 17 (не повышать/понижать), Spring Boot 4.0.x (НЕ даунгрейдить на 3.x), JJWT 0.12.x, Spring Data JPA, Flyway, PostgreSQL 16
- Frontend: React 18.3, TypeScript ~5.6 strict, Vite 5.4, react-router-dom 6.30, @dnd-kit; HTTP только через `fetch` в `frontend/src/api/client.ts` (не axios/React Query)
- Тесты backend (unit/IT): JUnit 5 + MockMvc, H2 профиль `test` с `MODE=PostgreSQL`; схема через Flyway + `ddl-auto=validate`
- E2E автотесты: отдельный каталог `autotests/` — Java 17, JUnit 5, Rest Assured (API), Selenide (UI); не смешивать с `backend/src/test`
- Локальные порты: backend часто `SERVER_PORT=8081` (default в yml — 8080); Vite 5173 проксирует `/api` и `/actuator` → `localhost:8081`
- Node 20+; prod frontend — nginx, проксирует `/api` на backend

## Critical Implementation Rules

### Language-Specific Rules

**TypeScript**
- Не ослаблять `strict` / unused / fallthrough в tsconfig
- HTTP только через `apiRequest`; ошибки — `ApiError` (status + details); fallback-тексты ошибок оставлять на русском, как в `client.ts`
- Типы контрактов — в `frontend/src/api/*`, рядом с функциями; не заводить параллельный `types/` и не дублировать DTO «на глаз»
- Не вызывать `fetch('/api...')` из pages/components

**Java**
- Только `jakarta.*` (не `javax.*`); без Lombok на entity/DTO
- Отступы — tabs (как в существующем коде)
- DTO — nested records в `*Dtos` + Validation на request-records
- Бизнес-ошибки — `BadRequestException` / `ConflictException` / `ResourceNotFoundException`; HTTP-статусы только в `ApiExceptionHandler`
- Constructor injection; `Instant` + UTC; не рассчитывать на Open Session in View (`open-in-view: false`) — загружать связи внутри `@Transactional` service

### Framework-Specific Rules

**React**
- Сессия только `AuthProvider` / `useAuth` + `sessionStorage` (`todolist.accessToken`); не localStorage/cookies
- Экраны в `pages/`, kanban в `components/kanban/`; board state — локальный; после create/update/move/delete синхронизировать с ответом API (не оставлять «оптимистичный» порядок без сервера)
- DnD только `@dnd-kit`; move идёт в существующий API `moveCard` / `PATCH .../move`
- Защищённые маршруты — через `ProtectedRoute`; новый API-метод сразу в `frontend/src/api/*`

**Spring**
- Controller → Service → Repository; Kanban API с `@PreAuthorize("hasRole('ADMIN')")` (не путать с `hasAuthority('ADMIN')`)
- Инвариант: у доски ровно 3 колонки со статусами `TODO` | `IN_PROGRESS` | `DONE` — нельзя добавлять/удалять статусы, только displayName/position
- Пользователь — `@AuthenticationPrincipal UserPrincipal`; JWT не парсить в controller вручную
- Схема — только Flyway `V*__*.sql`; `ddl-auto` остаётся `validate`
- Не «улучшать» CSRF/session policy и AdminBootstrap без явной задачи

### Testing Rules

- Изменение `/api/**` → расширять `AuthIntegrationTest` / `KanbanIntegrationTest` (или новый `*IntegrationTest`) с `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- Auth в IT — через реальный `POST /api/auth/login` + `Authorization: Bearer`; не заменять это `@WithMockUser` как основной способ
- Проверять happy path и 401/403/404/409; для ошибок — статус и JSON envelope (`message` / `details`), не только status
- Обязательные сценарии kanban при касании домена: 3 колонки, duplicate board name, move + порядок, доступ чужой доски
- Unit-тесты service допустимы для чистой логики, но не вместо MockMvc-IT для новых endpoint
- Frontend: не подключать Jest/Vitest/Playwright без отдельной задачи; gate — `npm run lint` и `npm run build`
- E2E (живые API/UI) — только в `autotests/` (Rest Assured / Selenide); не добавлять E2E в `backend/src/test` и не тащить Selenide в Spring-модуль
- Конфиг стенда E2E — `autotests/src/test/resources/autotest.properties` (defaults `:5173` / `:8081`); override через env/`-D`; при недоступном стенде — `assumeTrue`, не жёсткий fail инициализации

### Code Quality & Style Rules

- Frontend gate: `npm run lint`; не добавлять Prettier/другой форматтер и не делать массовый reformat существующих файлов в том же PR, что и фича
- Стили — через существующие `App.css` / `index.css` (или точечный CSS рядом по текущему паттерну); не подключать Tailwind/CSS-in-JS/UI-kit без задачи
- Java: tabs + стиль соседних классов; не подключать Spotless/Checkstyle «с нуля» в том же изменении, что доменная фича
- Именование: `*Controller`/`*Service`/`*Repository`/`*Dtos`/`*IntegrationTest`; FE — PascalCase компоненты, `*Page.tsx`, api-файлы `kanban.ts`/`auth.ts`; новые модули — named exports (кроме уже существующего `App` default)
- Код по доменным пакетам/папкам; не создавать свалку `utils/`/`helpers/`
- Flyway: только новый `V{n}__*.sql`; уже применённые V1/V2 не редактировать
- UI-тексты и fallback ошибок — на русском, в тоне существующих экранов

### Development Workflow Rules

- Не коммитить `.env` и секреты; шаблон — `.env.example`. В код не хардкодить JWT/пароли
- Два режима портов: локально Vite proxy `/api` → `localhost:8081` (`SERVER_PORT=8081`); Docker Compose backend обычно `:8080`. Не «унифицировать» порты, ломая proxy
- JDBC: native run часто `localhost`; compose — хост `db` из example. Не ломать один сценарий ради другого
- Изменение `/api` — вместе backend + `frontend/src/api` (+ IT); желательно синхронизировать `docs/api-contracts-backend.md`
- Перед сдачей: `backend` → `mvn test`; `frontend` → `npm run lint` && `npm run build`; при E2E-изменениях — `autotests` → `mvn test` на поднятом стенде
- Коммиты краткие в духе репо (`Add ...` / `Fix ...`); не добавлять CI/CD и branch-policy «заодно»

### Critical Don't-Miss Rules

- Нет signup/OAuth/новых ролей без задачи — bootstrap admin + Kanban под `ADMIN`
- Колонки: ровно статусы `TODO`/`IN_PROGRESS`/`DONE`; в JSON отображаемое имя — `name` (JPA `displayName`)
- Move: `targetStatus` + `targetIndex`; сервер нормализует `position` — клиент принимает серверный порядок
- Мутации board/cards идут через ownership + `findOwned*ForUpdate` (pessimistic lock); не убирать lock «для простоты»
- Чужие board/card id → как в коде (обычно 404), не открывать чужие данные и не менять на «более правильный» 403 без решения
- Имя доски уникально per author (409); title/name trim на сервере; пустой title после trim — ошибка
- Пароли BCrypt; секреты из env; наружу actuator только health; JWT не в URL и не в localStorage
- Синхрон FE/BE по `ColumnStatus`; не `ddl-auto=update`; не править старые Flyway; не ломать shape `ErrorResponse`

---

## Usage Guidelines

**Для AI-агентов:**
- Читать этот файл перед любой реализацией кода
- Следовать всем правилам как есть
- При сомнении выбирать более строгий вариант
- Обновлять файл, если появляются новые устойчивые паттерны

**Для людей:**
- Держать файл lean и сфокусированным на нуждах агентов
- Обновлять при смене стека или инвариантов домена
- Периодически вычищать правила, ставшие очевидными

Last Updated: 2026-07-22
