# Validation Report — BMAD Todo List: Параметры пользователей на карточках

- **PRD:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md`
- **Addendum:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md`
- **Rubric:** `.opencode/skills/bmad-prd@1.0/assets/prd-validation-checklist.md`
- **Run at:** 2026-07-22T12:00:00Z
- **Grade:** Good

## Overall verdict

Addendum написан дисциплинированно: дельта зафиксирована с явными границами, acceptance criteria
testable, бизнес-правила формализованы. Все семь измерений качества — strong или adequate, без
критических и высоких замечаний. Основные риски (3 medium, 3 low) связаны с неполнотой
рекомендаций в открытых вопросах (OQ-4: неправильная рекомендация для null/absent assigneeId;
OQ-2: отсутствие security-требований для GET /api/users) и отсутствием обязательного требования
синхронизации сборки CardResponse (RR-1/OQ-1). Addendum готов к передаче в downstream-шаг
архитектуры, но рекомендации OQ требуют верификации до начала реализации.

## Dimension verdicts

- Decision-readiness — adequate
- Substance over theater — strong
- Strategic coherence — adequate
- Done-ness clarity — strong
- Scope honesty — strong
- Downstream usability — adequate
- Shape fit — strong

## Findings by severity

### Critical (0)

None.

### High (0)

None.

### Medium (3)

**OQ-4: неполнота рекомендации по разграничению null/absent для assigneeId** (§9 OQ-4 addendum)

Рекомендация D (`@JsonInclude(NON_NULL)`) не решает заявленную проблему: Jackson десериализует
отсутствующее поле как null для Long, не различая «не передано» и «явный null». Семантика
«absent = не менять, null = сбросить» с этой рекомендацией нереализуема.

Fix: Дополнить OQ-4 указанием, что Jackson-дефолты не подходят. Рекомендовать отдельную DTO
с Optional&lt;Long&gt; или кастомный десериализатор.

---

**RR-1/OQ-1: риск рассинхронизации сборки CardResponse** (§4, §8, §11 addendum)

Addendum фиксирует риск RR-1 (дублирование toCardResponse в BoardService и CardService, высокая
вероятность), но OQ-1 лишь рекомендует общий mapper, не закрепляя обязательное требование.
Риск остаётся не-митигированным на уровне требований.

Fix: В FR-12 или affected areas добавить: «CardResponse собирается единым методом
(CardService.toResponse); BoardService делегирует этот метод, а не дублирует конструктор.»

---

**OQ-2: отсутствие требований безопасности для GET /api/users** (§8, §9 OQ-2 addendum)

Addendum рекомендует новый эндпоинт GET /api/users, но не специфицирует аутентификацию
(должен быть доступен только ADMIN) и объём возвращаемых данных (id, username — достаточно ли?).

Fix: Добавить в affected areas или AC: «GET /api/users требует аутентификации ADMIN и возвращает
id + username всех ADMIN. Неаутентифицированные запросы — 401/403.»

### Low (3)

**Non-Goals: неопределён механизм применения дельты к базовому PRD** (§2.3 addendum)

Addendum содержит изменения к Non-Goals §5 базового PRD, но механизм не определён (редактирование
base PRD status: final vs addendum как source of truth).

Fix: Зафиксировать: «При утверждении scope base PRD получает новую редакцию с изменениями из
§2.3, status: updated.»

---

**EC-4/PIA: политика cross-board assignee не полностью раскрыта** (§5 EC-4 addendum, §5 PIA)

EC-4 допускает cross-board assignee. BR-5 утверждает отсутствие прав доступа. Однако не
документировано, валидируется ли принадлежность assignee к доске при создании/назначении.

Fix: Явно указать в ограничениях: «assignee может быть любым ADMIN в системе, независимо от
владения доской. Права доступа определяются только Board.author.»

---

**AC-8: acceptance criteria для отображения на фронтенде не специфицирует формат** (§4 AC-8 addendum)

AC-8 специфицирует факт отображения (creatorUsername/assigneeUsername), но не формат (рядом
с заголовком, подвал карточки, тултип). Допустимо для UX-шага, но создаёт риск разногласий.

Fix: В AC-8 указать минимальные требования к расположению (например, «мелким шрифтом под
заголовком карточки»).

## Mechanical notes

- Глоссарий: термины Creator и Assignee добавлены в §1 addendum, согласованы с FR/AC.
- ID continuity: FR-11, FR-12 сохранены из базового PRD; BR, AC, EC, OQ — сплошная нумерация
  в пределах addendum.
- Cross-ссылки: addendum → PRD (§5, FR-11, FR-12, UJ-2) — все корректны.
- UJ: UJ-2 базового PRD расширяется (creator + assignee); новых UJ нет — корректно для дельты.
- Assumptions Index (§10): 8 assumptions, все обоснованы существующей архитектурой.
- Цитирования путей (backend/.../card/Card.java и т.д.) указаны как шаблоны — допустимо для PRD.

## Reviewer files

- `review-rubric.md`
