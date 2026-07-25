# Validation Report — BMAD Todo List + Addendum: Назначение исполнителя и создателя на карточку

- **PRD:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md`
- **Addendum:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/addendum.md`
- **Rubric:** `.opencode/skills/bmad-prd@1.0/assets/prd-validation-checklist.md`
- **Run at:** 2026-07-23T12:00:00Z
- **Grade:** Fair

## Overall verdict

Addendum компетентно фиксирует изменения, открытые вопросы и зоны влияния, но страдает от несбалансированной полноты: 10 Open Questions, 3 из которых затрагивают базовые бизнес-правила (BP-4, BP-5, BP-11), и полное отсутствие Success Metrics для новой функциональности. При этом Precondition OQ-5 (создание пользователей) корректно вынесен в Out of Scope, но его блокирующий характер для всего downstream недостаточно акцентирован. Документ пригоден для архитектурного обсуждения, но требует разрешения ключевых OQ и добавления SMs перед переходом к реализации.

## Dimension verdicts

- Decision-readiness — adequate
- Substance over theater — adequate
- Strategic coherence — thin
- Done-ness clarity — adequate
- Scope honesty — adequate
- Downstream usability — adequate
- Shape fit — adequate

## Findings by severity

### High (3)

**[Decision-readiness]** — **3 базовых бизнес-правила отмечены как открытые вопросы** (§3 BP-4, BP-5, BP-11)
BP-4 (доступ ко всей Доске), BP-5 (снятие доступа при снятии последнего assignee) и BP-11 (видимость всех карточек) имеют источник «Открытый вопрос». Три из одиннадцати бизнес-правил не фиксированы, что размывает контракт фичи.
Fix: Закрыть OQ-1, OQ-2, OQ-3 до или параллельно с архитектурной проработкой. Зафиксировать BP после решения PO.

**[Strategic coherence]** — **Нет Success Metrics для новой функциональности**
Родительский PRD содержит SM-1, SM-2, SM-3, SM-C1. Addendum не определяет ни одной метрики успеха для multi-user коллаборации (например, «100% карточек имеют creator; assignee установлен хотя бы на 20% карточек в первую неделю»). Без SMs невозможно верифицировать, что фича работает как задумано.
Fix: Добавить раздел Success Metrics в addendum (минимум 1 primary metric + 1 counter-metric для trade-off приватности).

**[Downstream usability]** — **Новые пользовательские сценарии (UJ-N1..N3) не формализованы в addendum**
PIA определяет три новых сценария (UJ-N1 просмотр Доски исполнителем, UJ-N2 назначение, UJ-N3 снятие), но addendum не включает их как структурированные UJ с named protagonist, пошаговым описанием и кульминацией. Родительский PRD имеет три полноценных UJ; их отсутствие в addendum снижает downstream-пригодность для UX и story creation.
Fix: Добавить раздел UJ (User Journeys) в addendum, повторяя стиль родительского PRD (UJ-1..UJ-3).

### Medium (3)

**[Scope honesty]** — **Нет Assumptions Index**
Addendum не содержит `[ASSUMPTION]`-тегов и их индексного списка. Неявные допущения: assignee использует ту же JWT-аутентификацию, тот же login-page, тот же механизм sessionStorage; не требуется новый Auth endpoint. Эти допущения должны быть явными.
Fix: Добавить `[ASSUMPTION]`-теги в текст addendum и собрать их в индекс в конце документа.

**[Substance over theater]** — **Риски (R-1..R-6) частично дублируют PIA**
Раздел §10 Risks содержит 6 записей, из которых R-2, R-3, R-5, R-6 уже описаны в Product Impact Assessment. Дублирование не вредно, но снижает плотность новой информации.
Fix: Сократить Risks до специфичных для addendum, на PIA сослаться по ref.

**[Mechanical notes]** — **Термин «Исполнитель» используется в двух значениях**
В Глоссарии addendum (п.2) «Исполнитель (Assignee)» — опциональный пользователь на карточке, а «Исполнитель (роль, не-ADMIN)» — роль. В бизнес-правилах и AC термин используется без уточнения, какое значение активно. В BP-6 — «Исполнитель НЕ может создавать/редактировать/удалять» — речь о роли, но AC-5.1 говорит об отображении исполнителя (концепт).
Fix: Ввести различную терминологию или явно разделять (Assignee vs Assignee-role), либо использовать «назначенный пользователь» для концепта.

### Low (2)

**[Mechanical notes]** — **Разнобой именований между Affected Areas и AC**
AC-6.1 говорит «Dashboard», а Affected Areas — «BoardPage.tsx». AC-6.2 «видит Доску целиком» — но OQ-1 эту видимость ставит под вопрос.
Fix: Синхронизировать терминологию между affected areas и AC; добавить примечание о зависимости AC от OQ.

**[Downstream usability]** — **OQ-5 не помечен как blocker**
Addendum корректно выносит создание пользователей в Out of Scope (§7) и OQ-5, но не маркирует его статусом BLOCKER или зависимостью «must have before feature implementation». Downstream может начать архитектурную проработку в расчёте на существование пользователей, но без endpoint/SQL фича нереализуема.
Fix: Добавить пометку `[BLOCKER]` к OQ-5 и/или вынести его в отдельный precondition-чеклист.

## Mechanical notes

- ID continuity: AC-1.x–AC-6.x — без пропусков и дубликатов. EC-1–EC-8 — полные.
- Cross-refs: Addendum корректно ссылается на родительский PRD (§5 Non-Goals, Глоссарий, FR-5). PIA не цитируется явно, хотя служил источником.
- Assumptions Index: отсутствует — Medium finding выше.
- UJ protagonist naming: UJ-N1..N3 не формализованы — High finding выше.
- Glossary drift: «Исполнитель» (концепт) vs «Исполнитель (роль, не-ADMIN)» — Medium finding выше.
- Open Questions density: 10 OQ + 0 `[ASSUMPTION]` для addendum статуса draft — плотность допустима, но 3 OQ затрагивают базовые BP, что повышает stake.

## Reviewer files

- `review-rubric.md` (встроено в данный отчёт)
