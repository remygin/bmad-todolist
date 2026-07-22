---
title: 'TEA Test Design → BMAD Handoff Document'
version: '1.0'
workflowType: 'testarch-test-design-handoff'
inputDocuments:
  - '_bmad-output/test-artifacts/test-design-architecture.md'
  - '_bmad-output/test-artifacts/test-design-qa.md'
sourceWorkflow: 'testarch-test-design'
generatedBy: 'TEA Master Test Architect (Murat)'
generatedAt: '2026-07-22T12:00:00Z'
projectName: 'bmad-todolist'
---

# TEA → BMAD Integration Handoff

## Purpose

Этот документ связывает test design outputs TEA (Master Test Architect) с workflow декомпозиции на epics и stories (`create-epics-and-stories`). Quality requirements, risk assessment и test strategy должны быть интегрированы в планирование имплементации feature «Параметры пользователей на карточках (creator + assignee)».

---

## TEA Artifacts Inventory

| Artifact | Path | BMAD Integration Point |
|---|---|---|
| Test Design Architecture | `_bmad-output/test-artifacts/test-design-architecture.md` | Epic quality requirements, testability blockers для Architecture/Dev |
| Test Design QA | `_bmad-output/test-artifacts/test-design-qa.md` | Story acceptance criteria, тестовые сценарии, P0/P1 приоритеты |
| Risk Assessment | (embedded в test-design-architecture.md §Risk Assessment) | Epic risk classification, story priority |
| Handoff (this doc) | `_bmad-output/test-artifacts/test-design/handoff.md` | Вход для create-epics-and-stories |

---

## Epic-Level Integration Guidance

### Risk References

Следующие риски должны быть отражены как epic-level quality gates:

| Risk ID | Score | Суть | Gate |
|---|---|---|---|
| R-01 | 9 | Рассинхронизация BoardService.toCardResponse | Pre-impl blocker: единый mapper до начала тестирования |
| R-05 | 6 | Валидация assigneeId (существование + ADMIN) | Validation epic: assigneeId 400 тесты в acceptance criteria |
| R-07 | 6 | V3 миграция backfill creator_id | Migration epic: тестовая миграция до деплоя |

### Quality Gates

| Gate | Epic | Критерий |
|---|---|---|
| G-01 | Backend: Card + DTO | CardResponse.from(Card) единый mapper реализован; BoardService.toCardResponse делегирует |
| G-02 | Backend: Card + DTO | UpdateCardRequest различает absent assigneeId vs null (resetAssignee) |
| G-03 | Backend: CardService | assigneeId валидация: 400 для несуществующего или не-ADMIN assigneeId |
| G-04 | Backend: Migration | V3 backfill creator_id = Board.author; NOT NULL после проверки |
| G-05 | Backend: User API | GET /api/users защищён @PreAuthorize; 401/403 |
| G-06 | Frontend | FE build + lint без ошибок; creator/assignee отображаются |

---

## Story-Level Integration Guidance

### P0/P1 Test Scenarios → Story Acceptance Criteria

Каждый из следующих тестовых сценариев ДОЛЖЕН быть acceptance criteria соответствующей story:

| Test ID | AC | Story | Acceptance Criterion |
|---|---|---|---|
| P0-001 | AC-1 | Story: Card create — assignee | Карточка создаётся без assigneeId: creator=текущий ADMIN, assignee=null |
| P0-002 | AC-2 | Story: Card create — assignee | Карточка создаётся с валидным assigneeId: creator + assignee заполнены |
| P0-003 | AC-3 | Story: Card create — assignee | Создание с несуществующим assigneeId → 400 |
| P0-004 | AC-5 | Story: Card update — assignee | Обновление assignee через PUT /cards/{id} сохраняется |
| P0-005 | AC-6 | Story: Card update — assignee | Сброс assignee (resetAssignee=true) возвращает null |
| P0-006 | — | Story: Board detail | Board.getBoard возвращает карточки с creatorId/creatorUsername/assigneeId/assigneeUsername |
| P1-003 | — | Story: GET /api/users | GET /api/users возвращает список ADMIN (id + username) |
| P1-004 | — | Story: GET /api/users | GET /api/users без токена → 401 |
| P1-005 | AC-7 | Story: V3 migration | V3 миграция: creator_id существующих карточек = Board.author_id |

### Data-TestId Requirements

Не требуется. FE отображение creator/assignee — простые `<span>` элементы без сложной интеракции. Если в будущем добавляются E2E-тесты, рекомендуется `data-testid="card-creator"` и `data-testid="card-assignee"`.

---

## Risk-to-Story Mapping

| Risk ID | Category | P×I | Recommended Story | Test Level |
|---|---|---|---|---|
| R-01 | TECH | 9 | Backend: BoardService — toCardResponse делегирует единому mapper | API IT |
| R-05 | DATA | 6 | Backend: CardService — assigneeId валидация (create + update) | API IT |
| R-07 | DATA | 6 | Backend: V3 migration — backfill + NOT NULL | Migration IT |
| R-03 | TECH | 4 | Backend: Card DTO — UpdateCardRequest assigneeId + resetAssignee | API IT |
| R-04 | BUS | 4 | Backend: API backward compatibility | API IT |
| R-06 | SEC | 3 | Backend: GET /api/users — @PreAuthorize | Security IT |

---

## Recommended BMAD → TEA Workflow Sequence

1. **TEA Test Design** (`TD`) ✓ — completed: produces this handoff + architecture + QA docs
2. **BMAD Create Epics & Stories** → consumes this handoff, embeds quality requirements
3. **BMAD Implementation** → developers implement per stories + acceptance criteria
4. **TEA ATDD** (`AT`) → generates failing acceptance tests per story (для P0/P1 сценариев)
5. **TEA Automate** (`TA`) → generates full test suite (P0-P3)
6. **TEA Trace** (`TR`) → validates coverage completeness + quality gate decision

---

## Phase Transition Quality Gates

| From Phase | To Phase | Gate Criteria |
|---|---|---|
| Test Design | Epic/Story Creation | Все P0 риски имеют mitigation strategy ✓ (R-01, R-05, R-07) |
| Epic/Story Creation | ATDD | Stories имеют acceptance criteria из test design |
| ATDD | Implementation | Failing acceptance tests существуют для всех P0/P1 сценариев |
| Implementation | Test Automation | Все acceptance tests проходят |
| Test Automation | Release | Trace matrix показывает ≥80% coverage P0/P1 requirements |
