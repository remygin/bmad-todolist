---
stepsCompleted:
  - step-01-document-discovery
  - step-02-prd-analysis
  - step-03-epic-coverage-validation
  - step-04-ux-alignment
  - step-05-epic-quality-review
  - step-06-final-assessment
date: '2026-07-18'
project: 'bmad-todolist'
assessor: 'Implementation Readiness (bmad-check-implementation-readiness)'
overallStatus: 'READY'
documentsIncluded:
  prd: 'prds/prd-bmad-todolist-2026-07-18/prd.md'
  architecture: 'architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md'
  epics: 'epics.md'
  ux: null
---

# Implementation Readiness Assessment Report

**Date:** 2026-07-18
**Project:** bmad-todolist

## Document Discovery

### PRD
- Package: `prds/prd-bmad-todolist-2026-07-18/`
- Primary: `prd.md` (30K, 2026-07-18)
- Companions: `reconcile-implementation.md`, `review-prose.md`, `review-rubric.md`, `review-structure.md`

### Architecture
- Package: `architecture/architecture-bmad-todolist-2026-07-18/`
- Primary: `ARCHITECTURE-SPINE.md` (13K, 2026-07-18)
- Companions: `reviews/` (adversarial, rubric, versions, reconcile-prd)

### Epics & Stories
- Whole: `epics.md` (32K, 2026-07-18)

### UX Design
- Not found

### Issues
- No whole/sharded duplicates
- UX document missing (warning)

## PRD Analysis

### Functional Requirements

FR-1: Вход по логину и паролю — неаутентифицированный посетитель может войти, отправив корректные логин и пароль, и получить Сессию (JWT). Consequences: корректные данные → токен и профиль с ролями; неверные → 401 без токена; пароли только BCrypt; TTL по умолчанию 1 час (`JWT_EXPIRATION_MS`); при загрузке недействительный токен удаляется, редирект на вход.

FR-2: Получение текущего администратора — Администратор может запросить свой профиль и Роли по действующей Сессии. Consequences: валидный токен → пользователь и роли; без/недействительный токен → 401.

FR-3: Выход из системы — Администратор может завершить работу, сбросив локальную Сессию. Consequences: после выхода защищённые экраны недоступны; выход удаляет JWT из `sessionStorage` и очищает локальный профиль; серверного отзыва токена нет.

FR-4: Автоматический bootstrap администратора — система при старте создаёт роль `ADMIN` и учётную запись Администратора из конфигурации, если их ещё нет. Consequences: на чистой БД — ровно одна учётная запись Администратора и одна Роль `ADMIN`; повторный старт не создаёт дубликатов.

FR-5: Список и просмотр досок — Администратор может получить список своих Досок и открыть Доску с Колонками и Карточками. Consequences: только свои Доски; чужая → 404; после загрузки списка первая Доска становится активной, если ранее активная не выбрана.

FR-6: Создание доски — Администратор может создать Доску с именем длиной до 120 символов. Consequences: ровно три Колонки (`TODO`, `IN_PROGRESS`, `DONE`); имя уникально без учёта регистра (409 при конфликте); пустое/длиннее 120 → 400.

FR-7: Переименование доски — Администратор может переименовать принадлежащую ему Доску. Consequences: те же требования к имени, что при создании; чужая/несуществующая → 404.

FR-8: Удаление доски — Администратор может удалить принадлежащую ему Доску после подтверждения в UI. Consequences: каскадное удаление Колонок и Карточек; чужая/несуществующая → 404.

FR-9: Инвариант трёх системных статусов — система гарантирует у любой Доски ровно три Колонки со Статусами `TODO`, `IN_PROGRESS`, `DONE`. Consequences: набор не из ровно трёх системных Статусов → 400; дублирование Статуса отклоняется.

FR-10: Переименование и переупорядочивание колонок — Администратор может атомарно сохранить полный набор из трёх Колонок с новыми отображаемыми именами и позициями (0..2). Consequences: имена и Позиции обновляются, Статусы неизменны; атомарная операция; имя непустое ≤80 символов иначе 400.

FR-11: Создание карточки — Администратор может создать Карточку в выбранной Колонке с заголовком и необязательным описанием. Consequences: указанный Статус, конец Колонки; пустой заголовок → 400; заголовок >200 или описание >4000 → 400.

FR-12: Редактирование карточки — Администратор может изменить заголовок и описание Карточки. Consequences: сохранение и возврат клиенту; те же лимиты длины; чужая/несуществующая → 404.

FR-13: Удаление карточки — Администратор может удалить Карточку после подтверждения в UI. Consequences: нормализация Позиций без «дыр»; чужая/несуществующая → 404.

FR-14: Перемещение карточки (drag-and-drop) — Администратор может переместить Карточку в другую Колонку и на нужную Позицию мышью, касанием или клавиатурой (drag-handle). Consequences: целевой Статус и Позиция; сервер атомарно применяет и нормализует; допустимая Позиция 0..размер целевой Колонки после извлечения; UI optimistic + rollback; при успехе UI не reread/не применяет ответ сервера.

FR-15: Health check — система предоставляет публичную проверку живости. Consequences: `GET /actuator/health` без Сессии → HTTP 200 с `status: "UP"`; детали внутренних компонентов не раскрываются.

**Total FRs: 15**

### Non-Functional Requirements

NFR-1 (feature §4.1): Все Kanban-операции требуют роли `ADMIN`; аутентификация без этой роли не даёт доступа к доскам и карточкам (403).

NFR-2 (feature §4.4): Мутации Досок и Карточек выполняются с проверкой владения и блокировкой записи, чтобы конкурентные операции не нарушали Позиции.

NFR-3 (безопасность): доступ к Kanban только с Ролью `ADMIN`; пароли — BCrypt; секреты из окружения; из служебных эндпоинтов извне — только health; JWT в `Authorization: Bearer`, хранение в `sessionStorage` (не localStorage/cookie/URL).

NFR-4 (целостность данных): доменные инварианты (три Статуса, уникальность имени Доски, нормализация Позиций) на сервере; при загрузке сервер — источник истины; move — optimistic с откатом, без немедленной синхронизации с ответом API.

NFR-5 (согласованность контрактов): REST-API — единый формат ошибок (`message` + `details`) с кодами 400/401/403/404/409; изменения `/api` синхронизируются между backend и frontend.

NFR-6 (доступность взаимодействия): перемещение Карточек работает мышью, касанием и клавиатурой (drag-handle).

NFR-7 (наблюдаемость): доступен health-эндпоинт для мониторинга живости.

NFR-8 (приватность): данные и учётная запись внутри развёртывания владельца; внешних сервисов и передачи данных наружу нет.

NFR-9 (граница безопасности): политика доступа, CSRF и управления Сессиями, bootstrap-Администратор — часть контракта; чужие ID не раскрывают данные.

NFR-10 (доменный инвариант): ровно три системных Статуса — архитектурный контракт UI и API; нарушение недопустимо.

**Total NFRs: 10**

### Additional Requirements

**Non-Goals (§5):** нет саморегистрации/смены/восстановления пароля/OAuth/SSO; нет multi-user и ролей кроме `ADMIN`; нет кастомных Статусов; нет вложений/комментариев/меток/сроков; нет публичного API за пределами админ-роли; нет native mobile; нет export/import/restore в продукте.

**Success Metrics:** SM-1 (инвариант трёх колонок), SM-2 (корректность серверного порядка после мутаций / воспроизведение после reload), SM-3 (приватность 401/403/404), SM-C1 (не наращивать число Статусов).

**Constraints / notes:** brownfield as-is PRD; расхождение с `project-context.md` по синхронизации UI после move (PRD приоритетен для текущего поведения); JWT TTL 1 час по умолчанию.

### PRD Completeness Assessment

PRD в статусе `final`, FR-1…FR-15 непрерывны с testable consequences, глоссарий и Non-Goals явные. Сопутствующие ревью (rubric/structure/prose/reconcile) подтверждают готовность к downstream; medium-риски: конфликт optimistic move с stale `project-context.md` (частично задокументирован в PRD). Для readiness UX-документ отсутствует — UX-требования частично встроены в FR-14/NFR-6.

## Epic Coverage Validation

### Epic FR Coverage Extracted

| FR | Epic | Story (по карте / составу) |
|----|------|----------------------------|
| FR1 | Epic 1 | Story 1.2 |
| FR2 | Epic 1 | Story 1.3 |
| FR3 | Epic 1 | Story 1.4 |
| FR4 | Epic 1 | Story 1.1 |
| FR5 | Epic 2 | Story 2.1 |
| FR6 | Epic 2 | Story 2.2 |
| FR7 | Epic 2 | Story 2.3 |
| FR8 | Epic 2 | Story 2.4 |
| FR9 | Epic 2 | Story 2.5 (инвариант) |
| FR10 | Epic 2 | Story 2.5 |
| FR11 | Epic 3 | Story 3.1 |
| FR12 | Epic 3 | Story 3.2 |
| FR13 | Epic 3 | Story 3.3 |
| FR14 | Epic 3 | Story 3.4 |
| FR15 | Epic 1 | Story 1.5 |

**Total FRs in epics: 15**

### Coverage Matrix

| FR Number | PRD Requirement | Epic Coverage | Status |
| --------- | --------------- | ------------- | ------ |
| FR-1 | Вход по логину и паролю | Epic 1 / Story 1.2 | ✓ Covered |
| FR-2 | Получение текущего администратора | Epic 1 / Story 1.3 | ✓ Covered |
| FR-3 | Выход из системы | Epic 1 / Story 1.4 | ✓ Covered |
| FR-4 | Bootstrap администратора | Epic 1 / Story 1.1 | ✓ Covered |
| FR-5 | Список и просмотр досок | Epic 2 / Story 2.1 | ✓ Covered |
| FR-6 | Создание доски | Epic 2 / Story 2.2 | ✓ Covered |
| FR-7 | Переименование доски | Epic 2 / Story 2.3 | ✓ Covered |
| FR-8 | Удаление доски | Epic 2 / Story 2.4 | ✓ Covered |
| FR-9 | Инвариант трёх статусов | Epic 2 / Story 2.5 | ✓ Covered |
| FR-10 | Переименование/переупорядочивание колонок | Epic 2 / Story 2.5 | ✓ Covered |
| FR-11 | Создание карточки | Epic 3 / Story 3.1 | ✓ Covered |
| FR-12 | Редактирование карточки | Epic 3 / Story 3.2 | ✓ Covered |
| FR-13 | Удаление карточки | Epic 3 / Story 3.3 | ✓ Covered |
| FR-14 | Перемещение карточки (DnD) | Epic 3 / Story 3.4 | ✓ Covered |
| FR-15 | Health check | Epic 1 / Story 1.5 | ✓ Covered |

### Missing Requirements

Нет. Критических и высокоприоритетных пропусков FR нет. FR в эпиках, отсутствующих в PRD, нет.

### Coverage Statistics

- Total PRD FRs: 15
- FRs covered in epics: 15
- Coverage percentage: 100%

## UX Alignment Assessment

### UX Document Status

**Not Found** — в `planning-artifacts` нет `*ux*.md` и sharded UX-пакета.

### Implied UI (yes)

Продукт — user-facing веб-SPA (login, список Досок, Kanban DnD мышь/касание/клавиатура). PRD UJ-1…UJ-3 и FR-14/NFR-6 явно задают UI-поведение. Epics фиксируют: «UX-документа нет — UX Design Requirements не извлекались»; продуктовые UX-поведения покрыты FR (auto-select первой доски, `window.confirm`, purge invalid JWT, optimistic move).

### Alignment Issues

Отдельного UX-документа нет — классическая UX↔PRD / UX↔Architecture сверка невозможна. Компенсация: UX-критичные поведения встроены в PRD FR и stories (особенно Story 3.4 DnD, 2.1 auto-select, 2.4/3.3 confirm).

### Warnings

- ⚠️ **WARNING:** UX Design document отсутствует при явно implied UI. Риск: визуальная иерархия, empty states, error presentation, responsive/layout patterns не зафиксированы отдельно от FR.
- Mitigating: brownfield as-is; UI уже реализован; ключевые interaction contracts (DnD modes, optimistic move, confirms) есть в PRD/epics.
- Рекомендация (не блокер FR coverage): при значимых UI-изменениях создать UX-спеку или принять осознанный waiver для as-is brownfield.

## Epic Quality Review

### Best Practices Compliance Checklist

| Epic | User value | Independence | Story sizing | No forward deps | ACs clear | FR traceability |
|------|------------|--------------|--------------|-----------------|-----------|-----------------|
| Epic 1: Вход и сессия | ✓ | ✓ stands alone | ✓ 5 stories | ✓ | ✓ GWT | FR1–4, FR15 |
| Epic 2: Доски и колонки | ✓ | ✓ needs only Epic 1 | ✓ 5 stories | ✓ | ✓ GWT | FR5–10 |
| Epic 3: Карточки | ✓ | ✓ needs Epic 1+2 | ✓ 4 stories | ✓ | ✓ GWT | FR11–14 |

### User Value Focus

- Epic 1–3 сформулированы как пользовательские исходы (вход/работа с досками/ведение карточек), не как «Setup DB / API / Infrastructure».
- Story 1.1 (bootstrap) и Story 1.5 (health) — операторские, не end-user Admin journeys; допустимы для self-hosted brownfield и привязаны к FR4/FR15.

### Epic Independence

- Epic 1 автономен (сессия + health).
- Epic 2 опирается только на аутентификацию Epic 1.
- Epic 3 опирается на Epic 1+2 (нужна Доска).
- Прямых или циклических зависимостей Epic N → Epic N+1 нет.

### Story Dependencies (within epic)

- Epic 1: 1.1 → 1.2 → 1.3 → 1.4; 1.5 независим. Нет ссылок вперёд.
- Epic 2: 2.1 → 2.2 → 2.3 → 2.4 → 2.5. Натуральный порядок CRUD → колонки.
- Epic 3: 3.1 → 3.2 → 3.3 → 3.4. Нет forward refs.

### Acceptance Criteria

- Все stories используют Given/When/Then; happy path и ошибки (400/401/404/409) покрыты.
- DnD (3.4): диапазон `targetIndex`, optimistic+rollback, отсутствие re-apply ответа — конкретны и тестируемы.
- Ownership 404 vs 403 согласован с PRD/AD-5.

### Greenfield / Brownfield / Starter

- Явно brownfield: starter template не требуется; схема через существующий Flyway, без «создать все таблицы в Story 1.1».
- Integration points: AD-1…AD-12 и deferred items (CI/CD, CORS, global 401, sync move response) вынесены — не смешивают Non-Goals с MVP stories.

### Quality Findings by Severity

#### 🔴 Critical Violations

Нет.

#### 🟠 Major Issues

Нет. Forward dependencies, technical-only epics и «epic-sized» stories не обнаружены.

#### 🟡 Minor Concerns

1. **Смешение операторских stories в Epic 1** — Story 1.1 (bootstrap) и 1.5 (health) рядом с login/logout; cohesion чуть слабее классического user-epic, но FR coverage и AC полные. *Remediation (optional):* оставить as-is для компактности brownfield или вынести health в тонкий Epic «Эксплуатация».
2. **FR9+FR10 в одной Story 2.5** — инвариант и rename/reorder совмещены; размер всё ещё разумный. *Remediation (optional):* разделить только если понадобится параллельная разработка.
3. **Нумерация FR1 vs FR-1** — эпики без дефиса, PRD с дефисом; семантика 1:1. *Remediation:* унифицировать при следующем редактировании.
4. **Stale project-context vs optimistic move** — epics корректно фиксируют as-is (AD-6 / Story 3.4) и defer sync; риск для агентов, читающих только `project-context.md`. *Remediation:* обновить project-context (уже отмечено в PRD review).

### Recommendations

1. Качество эпиков достаточно для Phase 4 / create-story — блокеров нет.
2. Перед реализацией изменений move-sync — синхронизировать `project-context.md` с PRD/epics.
3. UX-документ — warning (step 4), не quality defect эпиков.

## Summary and Recommendations

### Overall Readiness Status

**READY** (с неблокирующими предупреждениями)

PRD (`final`), Architecture Spine и Epics согласованы: 15/15 FR покрыты, critical/major дефектов качества эпиков нет. UX-документ отсутствует, но ключевые UI-контракты зафиксированы в PRD/stories. Продукт brownfield as-is — артефакты достаточны для Phase 4 (sprint planning / create-story / implementation).

### Critical Issues Requiring Immediate Action

Нет.

### Non-Blocking Warnings / Minor Issues

1. **UX Design document отсутствует** при implied UI (веб-SPA Kanban).
2. **`project-context.md` устарел** относительно optimistic move (PRD/epics приоритетны для as-is).
3. **Minor:** операторские stories (bootstrap, health) внутри Epic 1; нумерация FR1 vs FR-1.

### Recommended Next Steps

1. Перейти к sprint planning — FR coverage и epic quality позволяют.
2. Обновить `_bmad-output/project-context.md`, чтобы правило после move совпадало с PRD FR-14 / Story 3.4 (или явно пометить PRD приоритетным).
3. Опционально: UX-waiver для brownfield as-is или краткая UX-спека при планировании UI-изменений.
4. Опционально: унифицировать идентификаторы FR (`FR-1` vs `FR1`) между PRD и epics.

### Final Note

Оценка выявила **0 critical**, **0 major**, **4 minor/warning** пункта в категориях Document Discovery, UX Alignment и Epic Quality. Critical blockers перед implementation нет; findings можно закрыть улучшением артефактов или принять as-is для brownfield.

**Assessor:** bmad-check-implementation-readiness  
**Date:** 2026-07-18
