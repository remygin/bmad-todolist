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
  - '_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md'
---

# Test Design for Architecture: Параметры пользователей на карточках (creator + assignee)

**Purpose:** Architectural concerns, testability gaps, and NFR requirements. Контракт между QA и Engineering по тестируемости изменения.

**Date:** 2026-07-22
**Author:** Murat (Master Test Architect)
**Status:** Architecture Review Pending
**Project:** bmad-todolist
**PRD Reference:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md` (+ addendum)
**ADR Reference:** `_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md` (AD-6, AD-8, AD-11)

---

## Executive Summary

**Scope:** Добавление полей creator (NOT NULL, авто-установка из сессии) и assignee (nullable) на карточки. Backend: Card entity + DTOs + CardService + V3 миграция + GET /api/users. Frontend: отображение creator/assignee в KanbanCard, выбор assignee в CardForm.

**Architecture** (из ARCHITECTURE-SPINE + architecture-delta):
- **AD-6 уточнение:** CardService — единственный writer creator/assignee; moveCard не меняет assignee
- **AD-8 уточнение:** единый метод `CardResponse.from(Card)` для сборки DTO
- **AD-11 уточнение:** GET /api/users — read-only, не нарушает aggregate boundaries
- **Решение OQ-4:** UpdateCardRequest использует `Long assigneeId` + `boolean resetAssignee`
- **Решение OQ-2:** GET /api/users защищён `@PreAuthorize("hasRole('ADMIN')")`
- **Решение OQ-3:** Ошибки assigneeId — 400 BadRequestException

**Risk Summary:**
- **Всего рисков:** 8
- **High-priority (≥6):** 3 риска, требующие немедленной митигации
- **Test effort:** ~24 теста (~1.5 недели для 1 QA)

---

## Quick Guide

### 🚨 BLOCKERS — Team Must Decide (Can't Proceed Without)

**Pre-Implementation Critical Path:**

1. **B-01: Единый mapper CardResponse** — BoardService.toCardResponse дублирует конструктор CardResponse (RR-1, OQ-1). Без единого mapper карточки в Board response не получат creator/assignee. (owner: Backend)
2. **B-02: Jackson десериализация assigneeId (null vs absent)** — UpdateCardRequest требует различения «поле не передано» и «явный null». В архитектуре принят вариант с `resetAssignee` boolean, но реализация должна строго следовать контракту. (owner: Backend)

**Что нужно от команды:** Завершить эти 2 пункта до начала разработки, иначе тестирование заблокировано.

---

### ⚠️ HIGH PRIORITY — Team Should Validate (We Provide Recommendation, You Approve)

1. **R-05: Валидация assigneeId — существование User + роль ADMIN** — Рекомендация: 400 BadRequest при несуществующем или не-ADMIN assigneeId. (implementation phase, owner: Backend)
2. **R-07: V3 миграция — backfill creator_id + NOT NULL** — Рекомендация: backfill через UPDATE с JOIN на Board.author; SET NOT NULL после валидации. (implementation phase, owner: Backend)

**Что нужно от команды:** Проверить рекомендации и утвердить или предложить изменения.

---

### 📋 INFO ONLY — Solutions Provided (Review, No Decisions Needed)

1. **Test strategy:** API-level (MockMvc IT) — 70%, интеграционные/модульные — 20%, E2E (FE + BE) — 10%. Фокус на API, так как изменение серверное + FE отображение простое.
2. **Tooling:** JUnit 5 + MockMvc (существующая инфраструктура), ручная проверка FE через `npm run build`.
3. **Tiered CI/CD:** Every PR — все P0/P1 тесты (~10-15 мин); nightly — не требуется.
4. **Coverage:** ~24 теста (P0: 6, P1: 8, P2: 6, P3: 4) с risk-based классификацией.
5. **Quality gates:** Все P0 проходят, нет open P0/P1 багов, V3 миграция проверена на тестовой БД.

---

## For Architects and Devs — Open Topics 👷

### Risk Assessment

**Всего рисков:** 8 (3 high-priority ≥6, 2 medium, 3 low)

#### High-Priority Risks (Score ≥6) — IMMEDIATE ATTENTION

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner | Timeline |
|---------|----------|-------------|-------------|--------|-------|------------|-------|----------|
| **R-01** | **TECH** | Рассинхронизация BoardService.toCardResponse — карточки на Board detail без creator/assignee | 3 | 3 | **9** | Вынести единый метод `CardResponse.from(Card)`; BoardService делегирует | Backend | pre-impl |
| **R-05** | **DATA** | Валидация assigneeId — несуществующий User или не ADMIN не отклоняется | 2 | 3 | **6** | Валидация в CardService: findById + hasRole ADMIN → 400 | Backend | impl |
| **R-07** | **DATA** | V3 миграция — backfill creator_id может дать NULL на повреждённых данных | 2 | 3 | **6** | SET NOT NULL после проверки; тестовая миграция на копии данных | Backend | pre-impl |

#### Medium-Priority Risks (Score 3-5)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner |
|---------|----------|-------------|-------------|--------|-------|------------|-------|
| R-03 | TECH | Jackson null vs absent в UpdateCardRequest.assigneeId | 2 | 2 | 4 | resetAssignee boolean флаг; тесты на оба сценария | Backend |
| R-04 | BUS | Обратная совместимость — старый FE без assigneeId | 2 | 2 | 4 | assigneeId опционален; отсутствие = без изменений | Backend |

#### Low-Priority Risks (Score 1-3)

| Risk ID | Category | Description | Probability | Impact | Score | Action |
|---------|----------|-------------|-------------|--------|-------|--------|
| R-06 | SEC | GET /api/users раскрывает данные пользователей | 1 | 3 | 3 | @PreAuthorize + только id/username; single-user — низкий риск |
| R-10 | TECH | Нарушение инварианта трёх колонок | 1 | 3 | 3 | Не затронуто scope; регрессия в существующих тестах |
| R-08 | OPS | FE optimistic update расходится с assignee на сервере | 1 | 1 | 1 | move не меняет assignee — неактуально |

#### Risk Category Legend

- **TECH**: Technical/Architecture (flaws, integration, scalability)
- **SEC**: Security (access controls, auth, data exposure)
- **DATA**: Data Integrity (loss, corruption, inconsistency)
- **BUS**: Business Impact (UX harm, logic errors, revenue)
- **OPS**: Operations (deployment, config, monitoring)

---

### NFR Testability Requirements

| NFR Category | Threshold / Requirement | Current Design Support | Gap / Decision Needed | Planned Evidence |
|---|---|---|---|---|
| Security | GET /api/users доступен только ADMIN (401/403) | Supported — @PreAuthorize + stateless JWT | Нет | Security IT: 401 без токена, 403 без ADMIN |
| Security | assigneeId валидируется (существование + ADMIN) | Supported — CardService + BadRequestException | Нет | API IT: 400 на невалидный assigneeId |
| Data Integrity | creator NOT NULL, backfill корректен | Supported — V3 миграция + NOT NULL | Нет | Migration IT: проверка после V3 |
| Data Integrity | CardResponse имеет единый источник сборки | **GAP** — BoardService.toCardResponse дублирует конструктор | R-01 (Score 9) | BoardService IT: карточки содержат 4 новых поля |
| Maintainability | FE типы контрактов синхронизированы с BE | Supported — api/kanban.ts, api/users.ts | Нет | Сборка FE + lint |

**Unknown thresholds:** Не выявлены. NFR thresholds заданы в PRD/архитектуре.

**Assessment boundary:** Финальный PASS/CONCERNS/FAIL — после `nfr-assess` на основе evidence.

---

### Testability Concerns and Architectural Gaps

#### 🚨 ACTIONABLE CONCERNS — Architecture Team Must Address

##### 1. Blockers to Fast Feedback (WHAT WE NEED FROM ARCHITECTURE)

| Concern | Impact | What Architecture Must Provide | Owner | Timeline |
|---|---|---|---|---|
| **Единый CardResponse mapper** | Board.getBoard вернёт карточки без creator/assignee | Вынести `CardResponse.from(Card)` или `CardMapper`; BoardService.toCardResponse → делегирование | Backend | pre-implementation |
| **resetAssignee семантика** | Невозможно отличить «не менять assignee» от «сбросить» | UpdateCardRequest с `Long assigneeId` + `boolean resetAssignee`; Jackson настройка | Backend | pre-implementation |

##### 2. Architectural Improvements Needed (WHAT SHOULD BE CHANGED)

1. **Общий mapper для CardResponse**
   - **Current problem:** BoardService.toCardResponse дублирует конструктор CardResponse (RR-1, риск рассинхронизации)
   - **Required change:** Статический factory-метод `CardResponse.from(Card)` в CardDtos или отдельный `CardMapper.java`
   - **Impact if not fixed:** Данные creator/assignee не отображаются в Board detail
   - **Owner:** Backend
   - **Timeline:** pre-implementation

2. **Тестовые фикстуры User для multi-admin сценариев**
   - **Current problem:** IT используют один bootstrap ADMIN; для тестов assignee нужен второй ADMIN
   - **Required change:** Добавить seed второго User с ролью ADMIN в тестовую БД (через UserRepository или test sql)
   - **Impact if not fixed:** Невозможно протестировать сценарии cross-assignee
   - **Owner:** QA/Backend
   - **Timeline:** implementation

---

### Testability Assessment Summary

#### What Works Well

- ✅ API-first design — все сценарии тестируются через REST, MockMvc IT без браузера
- ✅ Существующая KanbanIntegrationTest — foundation для расширения
- ✅ V3 миграция атомарна, обратима, с backfill
- ✅ Single-user — не нужно управлять пользователями; assignee = тот же ADMIN или null
- ✅ Архитектура уже имеет pessimistic lock на Board (AD-5) — покрывает конкурентные обновления assignee
- ✅ LAZY fetch creator/assignee + @Transactional — standard practice

#### Accepted Trade-offs (No Action Required)

- **Null vs absent assigneeId через resetAssignee флаг** — допустимый компромисс; Jackson Optional<Long> спорен. Принято решением OQ-4.
- **Нет отдельного эндпоинта для смены assignee** — assignee через общий PUT /cards/{id}. L-4 из addendum.
- **Нет тестового seed endpoint** — тесты полагаются на API. Приемлемо для MockMvc.

---

### Risk Mitigation Plans (High-Priority Risks ≥6)

#### R-01: Рассинхронизация BoardService.toCardResponse (Score: 9) — CRITICAL BLOCKER

**Mitigation Strategy:**
1. Создать `CardResponse.from(Card card)` статический метод в CardDtos (или `CardMapper`)
2. BoardService.toCardResponse заменяет прямое создание вызовом `CardResponse.from(card)`
3. CardService.move также использует этот метод
4. Убедиться, что новых мест сборки CardResponse не появилось

**Owner:** Backend
**Timeline:** pre-implementation
**Status:** Planned
**Verification:** BoardService IT проверяет, что все карточки в BoardResponse содержат creatorId/creatorUsername/assigneeId/assigneeUsername

#### R-05: Валидация assigneeId на существование User + ADMIN (Score: 6) — HIGH

**Mitigation Strategy:**
1. В CardService.create: перед сохранением проверить userRepository.findById(assigneeId) + user.hasRole(ADMIN)
2. В CardService.update: та же валидация
3. Если пользователь не найден или не ADMIN → throw BadRequestException
4. assigneeId = 0 или отрицательное → 400 (Bean Validation)

**Owner:** Backend
**Timeline:** implementation
**Status:** Planned
**Verification:** IT createCard с несуществующим assigneeId → 400; IT с assigneeId не-ADMIN → 400

#### R-07: V3 миграция — backfill + NOT NULL (Score: 6) — HIGH

**Mitigation Strategy:**
1. V3.sql: ADD COLUMN creator_id (nullable) → UPDATE backfill → ALTER SET NOT NULL
2. Тестовая миграция на H2 c существующими данными
3. Проверить, что у всех существующих карточек Board.author NOT NULL
4. Резерв: V3.1 revert script (DROP COLUMN)

**Owner:** Backend
**Timeline:** pre-implementation
**Status:** Planned
**Verification:** IT после V3: у всех карточек creator_id IS NOT NULL и равен Board.author_id

---

### Assumptions and Dependencies

#### Assumptions

1. User entity и UserRepository существуют (созданы в V1/V2)
2. Board.author NOT NULL, используется для backfill creator_id
3. В системе не больше одного ADMIN (single-user v1) — assignee может быть только тот же admin или null
4. Pessimistic lock на Board (AD-5) покрывает конкурентные обновления assignee
5. LAZY fetch creator/assignee достаточен (загрузка в @Transactional)
6. Существующие тесты не сломаются от новых полей в CardResponse (нестрогое JSON-сравнение)
7. GET /api/users используется только для выпадающего списка assignee

#### Dependencies

1. Реализация единого CardResponse mapper — до начала IT
2. V3 миграция — до Card entity изменений
3. FE типы (api/kanban.ts) — после согласования DTO

#### Risks to Plan

- **Risk:** Backend PR не включает единый mapper → R-01 (Score 9)
  - **Impact:** Карточки на Board detail не показывают creator/assignee
  - **Contingency:** QA блокирует тестирование до исправления; альтернатива — тестировать CardController отдельно, без Board check

---

**End of Architecture Document**

**Next Steps for Architecture Team:**
1. Review Quick Guide (🚨/⚠️/📋) и приоритизировать blockers
2. Назначить owner'ов и сроки для high-priority рисков (≥6)
3. Валидировать assumptions
4. Предоставить feedback QA по testability gaps

**Next Steps for QA Team:**
1. Дождаться разрешения pre-implementation blockers (B-01, B-02)
2. См. companion QA doc (`test-design-qa.md`) для тестовых сценариев
3. Подготовить тестовые фикстуры UserFactory для multi-admin сценариев
