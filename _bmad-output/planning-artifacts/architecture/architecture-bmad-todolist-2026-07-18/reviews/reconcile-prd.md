# Reconcile: Architecture Spine ↔ final PRD

**Spine:** `_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md`  
**PRD:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md` (`status: final`)  
**Дата:** 2026-07-18  
**Режим:** read-only (spine не изменялся)

## Вердикт

Противоречий AD ↔ PRD нет. FR-1–FR-15 в целом привязаны через binds / Capability Map / AD-3–AD-12. Ниже — только требования PRD без покрытия AD или Consistency Conventions, и явные противоречия (пусто).

## Gaps: PRD без покрытия AD/convention

### G1 — Автовыбор первой доски (FR-5)

**PRD:** после загрузки списка первая Доска автоматически становится активной, если ранее активная не выбрана.

**Spine:** AD-5 / AD-12 покрывают ownership и nested read model; поведения «первая = активная» нет ни в AD, ни в conventions.

### G2 — Каскадное удаление доски (FR-8)

**PRD:** удаление Доски каскадно удаляет её Колонки и Карточки.

**Spine:** AD-5 / AD-11 говорят про ownership и writers, но не фиксируют каскад при delete Board.

### G3 — Подтверждение удаления в UI (FR-8, FR-13)

**PRD:** удаление Доски и Карточки — только после подтверждения в UI.

**Spine:** нет AD/convention про confirm перед destructive UI-действиями.

### G4 — Мультимодальный drag-and-drop (FR-14 + Cross-Cutting NFR)

**PRD:** перемещение мышью, касанием и клавиатурой через drag-handle.

**Spine:** Stack перечисляет `@dnd-kit/*`, но ни один AD и ни одна Consistency Convention не обязывают три модальности ввода.

### G5 — Очистка недействительного токена при инициализации UI (FR-1)

**PRD:** при загрузке приложения недействительный токен удаляется из Сессии, затем редирект на `/login`.

**Spine:** AD-3 фиксирует Bearer + `sessionStorage` + ProtectedRoute и отсутствие server revoke; правило init-time purge invalid token в AD/conventions отсутствует (Deferred закрывает только mid-session global 401 handler).

## Gaps: AD противоречат PRD

Нет.

## Вне scope gaps (не считаются)

- Capability Map / Deferred, явно согласованные с PRD (optimistic move без sync ответа, нет mid-session 401 handler, Non-Goals).
- Детали реализации вне FR/NFR (порты Vite/Compose, Flyway naming, Java style).
