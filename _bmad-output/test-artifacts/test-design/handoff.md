---
title: TEA → BMAD Handoff — System-Level Test Design
project: bmad-todolist
status: draft
created: 2026-07-23
updated: 2026-07-23
from_phase: ai-test-design (system-level)
to_phase: create-epics-and-stories
sources:
  - test-design-architecture.md
  - test-design-qa.md
  - _bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-23/architecture-delta.md
---

# TEA → BMAD Handoff: System-Level Test Design

## 1. Summary of Work Done

Выполнен system-level test design (Phase 3 BMAD TEA) для фичи «Назначение исполнителя (assignee) и создателя (creator) на карточку». Созданы два канонических документа:

| Документ | Назначение |
|----------|-----------|
| `test-design-architecture.md` | Архитектура тестирования: уровни, среда, данные, CI, контракты CT-1..CT-10, test entry points |
| `test-design-qa.md` | QA-дизайн: risk-based приоритизация, coverage matrix (FR/AC/BP/EC), тест-сценарии P0-P3, regression, NFR/security |

## 2. Key Findings

### 2.1 Testability Concerns

| Concern | Severity | Описание |
|---------|----------|----------|
| TC-1 | **BLOCKER** | Нет минимум 2 пользователей в системе — precondition assignee-сценариев. Без механизма создания пользователей (POST /api/users или SQL) тестирование CT-1..CT-8 невозможно |
| TC-2 | HIGH | Существующие Integration тесты сломаются при NOT NULL creator_id (RGR-4, R-TA-3). Требуется обновление fixture до или вместе с основными изменениями |
| TC-3 | MEDIUM | H2 MODE=PostgreSQL может некорректно обрабатывать EXISTS-подзапросы. Рекомендуется проверить поведение или заменить на COUNT > 0 |
| TC-4 | MEDIUM | FE Vitest-тесты не настроены в проекте (gate = lint + build). Роль assignee на FE остаётся без автоматизированной проверки |
| TC-5 | LOW | E2E-тесты (autotests/) не включены в CI pipeline — регрессия assignee-сценариев не ловится автоматически |

### 2.2 Risk Register (для epics)

| Risk | Severity | Epic |
|------|----------|------|
| R-TA-1: Access control — assignee получает мутации | Critical | Epic 1 (Backend) |
| R-TA-2: Утечка данных — assignee видит чужие доски | Critical | Epic 1 (Backend) |
| R-TA-3: NOT NULL creator_id ломает существующие тесты | High | Epic 5 (Regression) |
| R-TA-4: Нарушение атомарности (card + access) | High | Epic 1 (Backend) |
| R-TA-5: Некорректное снятие assignee доступа | Medium | Epic 1 (Backend) |
| R-TA-6: FE показывает мутации assignee | Critical | Epic 3 (Frontend) |

### 2.3 Test Entry Points (для create-epics-and-stories)

| EP | Entry Point | Source | Epic |
|----|-------------|--------|------|
| EP-1 | Backend: Card entity + repository + Flyway V3 | architecture-delta §4.1, §4.5 | Epic 1 |
| EP-2 | Backend: CardService — assignee/creator логика | architecture-delta §4.2 | Epic 1 |
| EP-3 | Backend: BoardService — расширение доступа | architecture-delta §4.2 | Epic 1 |
| EP-4 | Backend: UserController — GET /api/users | architecture-delta §4.3 | Epic 1 |
| EP-5 | Backend: Integration tests CT-1..CT-10 | architecture-delta §5.3 | Epic 2 |
| EP-6 | FE: Avatar component | architecture-delta §4.4 | Epic 3 |
| EP-7 | FE: Card / CardForm — assignee field | architecture-delta §4.7 | Epic 3 |
| EP-8 | FE: BoardPage — role adaptation | architecture-delta §4.4 | Epic 3 |
| EP-9 | FE: User API module + types | architecture-delta §4.7 | Epic 3 |
| EP-10 | Precondition: создание пользователей | addendum OQ-5 | Epic 4 (BLOCKER) |
| EP-11 | Regression: fixtures, existing tests | RGR-4, R-TA-3 | Epic 5 |

## 3. Recommended Epic Structure

```
Epic 1 — Backend: Card assignee/creator (EP-1, EP-2, EP-3, EP-4)
├── Story 1.1: Flyway V3 migration (cards.creator_id, cards.assignee_id)
├── Story 1.2: Card entity — creator (NOT NULL) + assignee (nullable)
├── Story 1.3: CardService — create/update assignee logic + access grant
├── Story 1.4: BoardService — access check (author OR EXISTS assignee)
├── Story 1.5: UserController — GET /api/users (ADMIN-only)
└── Story 1.6: Card DTO — creatorId, creatorUsername, assigneeId, assigneeUsername

Epic 2 — Backend: Integration Tests (EP-5)
├── Story 2.1: CardAssigneeIntegrationTest (CT-1..CT-5)
├── Story 2.2: BoardAccessIntegrationTest (CT-6, CT-7)
├── Story 2.3: UserControllerIntegrationTest (CT-8)
└── Story 2.4: CardMoveIntegrationTest (CT-9)

Epic 3 — Frontend: Assignee UI (EP-6, EP-7, EP-8, EP-9)
├── Story 3.1: Avatar component
├── Story 3.2: Card — display creator/assignee
├── Story 3.3: CardForm — assignee dropdown
├── Story 3.4: BoardPage — role adaptation (ADMIN vs assignee)
├── Story 3.5: User API module (getUsers)
└── Story 3.6: Types update (api/kanban.ts)

Epic 4 — Precondition: User creation (EP-10) [BLOCKER]
├── Story 4.1: POST /api/users endpoint (ADMIN-only)
└── Story 4.2: Тестовые фикстуры (multi-user.sql)

Epic 5 — Regression (EP-11)
├── Story 5.1: Обновление test/data.sql для creator_id
└── Story 5.2: KanbanRegressionIntegrationTest (CT-10)
```

## 4. Dependencies and Order

```
EP-10 (User creation) ──BLOCKER──→ Epic 1 ──→ Epic 2
                                         └──→ Epic 3
Epic 5 может выполняться параллельно с Epic 1
```

## 5. Open Questions (для PM / create-epics-and-stories)

| OQ | Вопрос | Эскалация |
|----|--------|-----------|
| OQ-H-1 | Механизм создания пользователей — POST /api/users или SQL? | PO / Architect |
| OQ-H-2 | FE Vitest-тесты — включать в Epic 3 или deferred? | PM |
| OQ-H-3 | E2E-тесты в autotests/ — в какой epic? | PM |
| OQ-H-4 | Добавлять ли Success Metrics для multi-user коллаборации (PO-Q1)? | PO |
| OQ-H-5 | Формализация User Journeys (PO-Q2)? | PM / UX |

## 6. Metrics for Epic Success

| Epic | Success Criteria |
|------|-----------------|
| Epic 1 | Все CT-1..CT-9 проходят; assignee не может мутировать; чужие доски → 404 |
| Epic 2 | P0-P1 сценарии покрыты Integration Tests; CT-10 pass (regression) |
| Epic 3 | Assignee не видит UI мутаций; Avatar рендерится корректно |
| Epic 4 | POST /api/users создаёт пользователя; fixture multi-user.sql доступна |
| Epic 5 | Существующие тесты проходят с новыми fixture |
