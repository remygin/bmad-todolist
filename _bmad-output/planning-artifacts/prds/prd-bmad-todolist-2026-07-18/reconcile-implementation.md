# Сверка brownfield PRD с реализацией

**PRD:** `_bmad-output/planning-artifacts/prds/prd-bmad-todolist-2026-07-18/prd.md`  
**Дата сверки:** 2026-07-18  
**Режим:** read-only (PRD не изменялся)  
**Источники:** `README.md`, `docs/*.md`, `_bmad-output/project-context.md`, `backend/src/**`, `frontend/src/**`

## Вердикт

PRD в целом корректно фиксирует as-is продукт: JWT-админ, ownership досок/карточек, CRUD, инвариант трёх колонок, DnD, отсутствие прикладных лимитов на количество сущностей и отсутствие export/backup. Существенное расхождение одно: **FR-11 / SM-2 заявляют принятие серверного порядка после move, тогда как UI применяет оптимистичный локальный порядок и не синхронизирует доску с ответом API при успехе.** Остальное — уточнения длин полей и мелкие UX-пробелы.

---

## 1. Особо проверенные зоны

### 1.1 JWT TTL и поведение истёкшей сессии

| Утверждение PRD | Факт | Статус |
|-----------------|------|--------|
| TTL по умолчанию 1 час (`3600000` мс) | `application.yml`: `app.jwt.expiration-ms: ${JWT_EXPIRATION_MS:3600000}`; `.env.example` / compose — то же | ✅ Верно |
| Настраивается через `JWT_EXPIRATION_MS` | Да | ✅ Верно |
| При инициализации UI недействительный токен удаляется, редирект на `/login` | `AuthContext`: `fetchMe` → on error `clearToken()`; `ProtectedRoute` → `/login` | ✅ Верно |
| Нет централизованного перехвата 401 во время открытой страницы | `api/client.ts` только бросает `ApiError`; logout/redirect по 401 mid-session нет | ✅ Верно |
| Ошибка показывается в контексте операции | `BoardsPage` / карточки показывают `error` | ✅ Верно |

**Нюанс (не ошибка PRD):** при mid-session 401 токен остаётся в `sessionStorage`, `user` в Context не сбрасывается — UI остаётся «вошедшим», пока не перезагрузка/logout. Это согласуется с формулировкой §4.1.

### 1.2 Storage токена

| Утверждение PRD | Факт | Статус |
|-----------------|------|--------|
| JWT в `Authorization: Bearer` | `client.ts` | ✅ |
| Хранение в `sessionStorage`, не `localStorage` / cookie / URL | ключ `todolist.accessToken` | ✅ |
| Logout = локальный сброс сессии | `clearToken()` + `setUser(null)`; серверного logout нет | ✅ |

Согласовано с `docs/integration-architecture.md`, `project-context.md`.

### 1.3 Ownership

| Утверждение PRD | Факт | Статус |
|-----------------|------|--------|
| Доски только текущего автора; чужие → 404 | `BoardRepository.findByIdAndAuthorId` / `findOwnedByIdForUpdate` | ✅ |
| Карточки через владение доской; чужие → 404 | `CardRepository.findByIdAndBoardAuthorId` | ✅ |
| Kanban только с ролью `ADMIN` (403 без роли) | `@PreAuthorize("hasRole('ADMIN')")` на controllers | ✅ |
| Мутации под write-lock | `findOwned*ForUpdate` (pessimistic) | ✅ |

### 1.4 CRUD досок / карточек

| Возможность | Backend | Frontend | PRD | Статус |
|-------------|---------|----------|-----|--------|
| Список / create / get / rename / delete boards | ✅ | ✅ (`BoardsPage`) | FR-5 | ✅ |
| Имя уникально per author (409) | case-insensitive check + DB unique | ✅ | FR-5 | ✅ (case-insensitive в PRD не уточнено) |
| Пустое имя после trim → 400 | ✅ | client trim | FR-5 | ✅ |
| Каскадное удаление доски | FK `ON DELETE CASCADE` | `window.confirm` | FR-5 | ✅ |
| Create / update / delete / move cards | ✅ | ✅ | FR-8..11 | ✅ (см. §2.1 про move UI) |
| Позиции нормализуются на сервере | `CardService.normalize` | — | Глоссарий / FR | ✅ |

### 1.5 Три колонки

| Утверждение PRD | Факт | Статус |
|-----------------|------|--------|
| Новая доска → ровно 3 колонки `TODO` / `IN_PROGRESS` / `DONE` | `BoardService.create` | ✅ |
| Нельзя добавить/удалить/заменить статусы | `validateColumnConfiguration` | ✅ |
| Rename + reorder атомарно (PUT full set) | `configureColumns` | ✅ |
| UI «Колонки» | `ColumnSettings` | ✅ |

**Нюанс:** дефолтные `displayName` при создании — `"Todo"`, `"In progress"`, `"Done"` (EN). PRD в сценариях приводит русские примеры как результат переименования — не ложь, но дефолт в PRD не зафиксирован.

### 1.6 DnD input modes

| Утверждение PRD | Факт | Статус |
|-----------------|------|--------|
| Мышь | `PointerSensor` | ✅ |
| Касание (touch) | через Pointer Events + `touch-action: none` на handle; отдельного `TouchSensor` нет | ✅ по смыслу |
| Клавиатура за drag-handle | `KeyboardSensor` + `sortableKeyboardCoordinates`; listeners только на `.drag-handle` | ✅ |

Согласовано с README / `docs/component-inventory-frontend.md`.

### 1.7 Лимиты

| Утверждение PRD (§10) | Факт | Статус |
|-----------------------|------|--------|
| Нет прикладных лимитов на число досок/карточек | В коде нет max-count / rate-limit на сущности | ✅ |
| Есть ограничения длины полей (PRD: «ограничено по длине», без чисел) | name доски ≤120; имя колонки ≤80; title ≤200; description ≤4000 | ⚠️ пробел конкретики |

### 1.8 Backup / export

| Утверждение PRD (§10) | Факт | Статус |
|-----------------------|------|--------|
| Export / restore в продукте отсутствуют | Нет API/UI export/import/backup | ✅ |
| Backup — зона эксплуатации развёртывания | Только PostgreSQL volume в compose | ✅ |

---

## 2. Расхождения и пробелы

### 2.1 [P1] FR-11 / SM-2: «серверный порядок», а не оптимистичный

**PRD:**  
- FR-11: «Клиент отображает серверный порядок после перемещения, а не собственный оптимистичный.»  
- SM-2: позиции совпадают между сервером и клиентом после мутации.  
- `project-context.md`: после move синхронизировать с ответом API.

**Реализация (`BoardsPage.handleMoveCard`):**
1. Сразу применяет `moveCardLocally` (оптимистичный UI).
2. Вызывает `PATCH /api/cards/{id}/move`.
3. При **успехе** ответ `CardResponse` **игнорируется**, доска не перезагружается.
4. При ошибке откат к `previous`.

**Вывод:** требование в PRD **не соответствует as-is**. Сервер нормализует позиции; клиент после успешного move живёт на локальной модели. Для brownfield-PRD формулировку нужно смягчить до фактического optimistic + rollback, либо отдельно пометить как gap реализации относительно intent в `project-context`.

### 2.2 [P2] Числовые лимиты длины полей не зафиксированы

PRD говорит «ограничено по длине», но не даёт контрактных значений, уже жёстко зашитых в DTO/`@Size` и схему:

| Поле | Max |
|------|-----|
| Board.name | 120 |
| Column.displayName / request `name` | 80 |
| Card.title | 200 |
| Card.description | 4000 |

Для testable consequences FR-5/FR-8 это пробел спецификации (не выдумка).

### 2.3 [P2] Опущенные существенные UX-поведения

Реализовано в UI, в PRD не отражено (или отражено слабо):

| Поведение | Где | Почему важно |
|-----------|-----|--------------|
| Подтверждение удаления **карточки** (`window.confirm`) | `KanbanCard` | FR-10 не упоминает confirm (только доска в FR-5) |
| Автовыбор первой доски при загрузке | `BoardsPage` useEffect | Часть UJ-1 «видит список своих досок» — фактически сразу открывается первая |
| Создание карточки **в любой** колонке (не только TODO) | `KanbanColumn` + API `status` | UJ-2 описывает путь «в к выполнению»; FR-8 корректнее («выбранная колонка») |

### 2.4 [P3] Мягкая неточность видения vs Non-Goals

§1: «для себя и своей команды на своём сервере» при том, что §2.2 / §5 явно исключают многопользовательский доступ. Не функциональная выдумка, но риск неверной интерпретации scope.

### 2.5 Что проверено и **не** является расхождением

- Нет выдуманных FR про signup / OAuth / кастомные статусы / вложения / публичный multi-tenant API.
- Закрытые решения §10 по лимитам количества и backup/export — верны.
- Health только `/actuator/health` наружу — верно.
- Error envelope `message` + `details` — верно.
- Bootstrap ADMIN — верно.
- DnD mouse/touch/keyboard за handle — верно по смыслу.

---

## 3. Матрица покрытия FR

| FR | Тема | Соответствие коду |
|----|------|-------------------|
| FR-1 | Login + JWT TTL + init clear | ✅ |
| FR-2 | `/api/auth/me` | ✅ |
| FR-3 | Logout локальный | ✅ |
| FR-4 | AdminBootstrap | ✅ |
| FR-5 | Boards CRUD + unique + cascade + confirm | ✅ (длины без чисел) |
| FR-6 | Инвариант 3 статусов | ✅ |
| FR-7 | Rename/reorder колонок | ✅ |
| FR-8 | Create card | ✅ |
| FR-9 | Update card | ✅ |
| FR-10 | Delete card + normalize | ✅ (confirm UI не в PRD) |
| FR-11 | Move DnD | ⚠️ sensors ✅; «не оптимистичный» ❌ |
| NFR ownership + lock | | ✅ |
| NFR sessionStorage | | ✅ |
| Health | MVP 6.1 | ✅ |

---

## 4. Рекомендации к правке PRD (не выполнены)

1. **FR-11 / §4.1 / SM-2:** описать фактический optimistic move + rollback on error; убрать утверждение, что клиент всегда принимает серверный порядок после move (пока код не синхронизирует).
2. **FR-5 / FR-7 / FR-8:** добавить числовые max длины полей.
3. **FR-10 / UJ-1:** упомянуть confirm удаления карточки и автооткрытие первой доски.
4. **§1 Видение:** заменить «команды» на формулировку single-admin / self-hosted, согласованную с Non-Goals.

---

## 5. Итог

| Категория | Кол-во |
|-----------|--------|
| Критичные неверные as-is утверждения | 1 (optimistic move) |
| Пробелы конкретики / UX | 2–3 |
| Выдуманные возможности (signup, export, multi-user API и т.п.) | 0 |
| Ошибочные утверждения по JWT / storage / ownership / 3 колонки / limits count / backup | 0 |

**Общая оценка:** brownfield-PRD пригоден как baseline после правки FR-11 и добавления числовых лимитов полей.

