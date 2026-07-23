# Readiness Report — BMAD Todo List: Назначение исполнителя и создателя на карточку

**Вердикт: CONCERNS**

**Дата проверки:** 2026-07-23
**Источники:** PRD (final), Addendum (draft), Architecture-delta (draft), ARCHITECTURE-SPINE (final), Epics (updated), test-design-architecture (draft), test-design-qa (draft)

---

## 1. Чеклист согласованности

### 1.1 PRD ↔ Architecture-delta ↔ Epics

| Проверка | Статус | Комментарий |
|----------|--------|-------------|
| PRD §5 Non-Goals отменены в addendum | PASS | addendum §1 явно отменяет п. «назначения исполнителей» |
| Глоссарий расширен (Creator/Assignee) | PASS | addendum §2 + architecture-delta §4.1 |
| Бизнес-правила BP-1..BP-11 | PASS | Отражены в architecture-delta §4.2 и epics Story 5.2 |
| Приёмочные критерии AC-1..AC-6 | PASS | Покрыты в epics Story 5.1–5.6 и test-design-qa §2.1 |
| Edge Cases EC-1..EC-8 | PASS | Покрыты в epics Story 5.2 и test-design-qa §2.2 |
| AD-3 (Authn/Authz) amendment | PASS | Architecture-delta §1.4, spine AD-3 расширен |
| AD-5 (Ownership) amendment | PASS | Architecture-delta §1.4, spine AD-5 расширен |
| AD-13..AD-15 (new ADs) | PASS | Внесены в spine |
| FR coverage map | PASS | Epics FR Coverage Map — полное покрытие FR-A1..FR-A8 |
| User Journeys для multi-user | CONCERNS | Не формализованы (deferred), но epics покрывают сценарии |

### 1.2 PRD/Addendum — открытые вопросы (OQ)

| OQ | Статус | Комментарий |
|----|--------|-------------|
| OQ-1 (доступ ко всей доске) | ЗАКРЫТ | Architecture-delta §4.8: вся доска целиком |
| OQ-2 (снятие последнего assignee) | ЗАКРЫТ | Architecture-delta §4.8: доступ теряется немедленно (EXISTS) |
| OQ-3 (UI выбора assignee) | ЗАКРЫТ | Architecture-delta §4.8: выпадающий список (GET /api/users) |
| OQ-4 (удаление пользователя) | ЗАКРЫТ | Architecture-delta §4.8: SET NULL для assignee; запрет для creator |
| **OQ-5 (создание пользователей)** | **CONCERNS** | Архитектурно закрыт как precondition; Epics включают POST /api/users (Story 4.1). Addendum §7 остаётся открытым (требуется решение PO) |
| OQ-6 (поля GET /api/users) | ЗАКРЫТ | id + username |
| OQ-7 (пагинация) | ЗАКРЫТ | Deferred for MVP |
| OQ-8 (FE ADMIN vs assignee) | ЗАКРЫТ | JWT roles |
| OQ-9 (FetchType) | ЗАКРЫТ | LAZY + @EntityGraph |
| OQ-10 (move + assignee) | ЗАКРЫТ | Не затрагивает |

### 1.3 Тест-дизайн

| Проверка | Статус | Комментарий |
|----------|--------|-------------|
| Contract tests CT-1..CT-10 | PASS | Определены в architecture-delta §5.3 и test-design-architecture §9 |
| Risk coverage (R-TA-1..R-TA-9) | PASS | P0/P1 сценарии выделены, mitigation описаны |
| Regression scope (RGR-1..RGR-8) | PASS | Учтены: fixture update, миграция, null assignee, move |
| FE автоматизация (Vitest) | **CONCERNS** | FE gate = lint + build; Vitest-тесты deferred. Critical P0-6 (assignee видит/не видит UI) — без автоматизации |
| E2E (autotests/) | PASS | Deferred — не блокирует |
| Precondition тестовых данных | **BLOCKER** | Без минимум 2 пользователей assignee-тесты не запустить (CT-1..CT-8) |

### 1.4 Rollback

| Аспект | Статус | Комментарий |
|--------|--------|-------------|
| Backend rollback | PASS | Откат V3 → V2 + code rollback |
| Frontend rollback | PASS | Откат до предыдущей версии |
| Data loss risk | PASS | Принят: creator_id не восстанавливается при откате V3 |
| Addendum упоминание | CONCERNS | Addendum не упоминает rollback-стратегию |

---

## 2. Риски и зависимости

### 2.1 Критические риски (P0)

| ID | Риск | Severity | Mitigation |
|----|------|----------|------------|
| R-TA-1 | Assignee получает доступ к мутациям | Critical | @PreAuthorize("hasRole('ADMIN')") на мутациях |
| R-TA-2 | Утечка данных: assignee видит чужие доски | Critical | EXISTS-подзапрос + author_id OR assignee |
| R-TA-6 | FE показывает мутации assignee | Critical | Условный рендеринг (isAdmin) — без автоматизации |
| P0-4 | NOT NULL creator при создании карточки | Critical | Автоматическое заполнение из @AuthenticationPrincipal |

### 2.2 Зависимости

| # | Зависимость | Блокирует |
|---|------------|-----------|
| 1 | Flyway V3 (миграция БД) | Backend-изменения Card |
| 2 | Backend: Card entity + repository | CardService |
| 3 | Backend: UserController/UserService (GET /api/users) | FE getUsers() |
| 4 | **POST /api/users или SQL-фикстура (precondition)** | Тестирование assignee-сценариев |
| 5 | FE: api/kanban.ts типы | FE Card/CardForm/BoardPage |

### 2.3 Blocker

**Отсутствие механизма создания пользователей** (OQ-5) — без реализации `POST /api/users` (Story 4.1) или SQL-фикстуры assignee-сценарии (CT-1..CT-8) нетестируемы. Epic 4 (Story 4.1) включён в план, но addendum §7 до сих пор помечает OQ-5 как требующий решения PO.

---

## 3. Открытые вопросы

**1. Синхронизация Addendum с архитектурными решениями**
- Addendum §9 (OQ-5) помечен как «требуется решение PO», хотя architecture-delta §4.8 уже закрыл его как `POST /api/users` (precondition, отдельная задача).
- Рекомендуется обновить addendum: проставить статус OQ-5 как «решено» и указать решение.

**2. FE автоматизация BoardPage role adaptation (P0)**
- FE gate = только `npm run lint` + `npm run build`. Критический сценарий P0-6 (assignee видит UI мутаций) не покрыт автоматическими тестами.
- Рекомендуется добавить минимальный Vitest-тест для BoardPage (условный рендеринг по isAdmin) в рамках Epic 5, Story 5.5.

**3. Success Metrics для multi-user коллаборации**
- PRD validation finding (architecture-delta §1.3) отложен. Рекомендуется добавить SM-4 (корректность access control) перед или в рамках реализации.

---

## 4. Итоговый вердикт

| Компонент | Оценка |
|-----------|--------|
| PRD — Addendum согласованность | CONCERNS (OQ-5 не синхронизирован) |
| Architecture — Spine согласованность | PASS |
| Epics — FR/AC coverage | PASS |
| Test-design — regression scope | PASS |
| Test-design — FE automation | CONCERNS (Vitest deferred для P0-6) |
| Rollback strategy | PASS |
| Риски/зависимости | CONCERNS (OQ-5 blocker не разрешён формально) |

**Итог: CONCERNS**

Рекомендуется разрешить вопросы 1–3 перед переходом к реализации. Основной blocker — формальное неподтверждение механизма создания пользователей (OQ-5), хотя архитектурное решение принято и Epic 4 включён в план.
