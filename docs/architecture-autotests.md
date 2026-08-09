# Архитектура — Autotests

## Краткое резюме

Отдельный Maven-проект для **E2E / системных** автотестов против поднятого стенда. Unit и интеграционные тесты кода остаются в `backend/` (MockMvc + H2) и не переносятся сюда.

## Стек

| Категория | Технология | Версия |
|-----------|------------|--------|
| Язык | Java | 17 |
| Сборка | Maven | 3.9+ |
| Тест-раннер | JUnit 5 | 5.12.x |
| API | Rest Assured | 5.5.x |
| UI | Selenide | 7.9.x |
| Assert | AssertJ | 3.27.x |

## Паттерн архитектуры

**Test harness (не приложение):**
- `config/` — URL, креды, проверка доступности стенда (`StackAvailability`)
- `api/` — HTTP-клиент Rest Assured (`ApiClient`)
- `ui/pages/` — Page Object (Selenide)
- `ui/SelenideSetup` — headless Chrome, baseUrl, timeouts
- `tests/api`, `tests/ui` — сценарии

Smoke-тесты при недоступном стенде **пропускаются** (`assumeTrue`), а не падают.

## Конфигурация

`src/test/resources/autotest.properties`:

| Ключ | Default (локально) |
|------|--------------------|
| `ui.baseUrl` | `http://localhost:5173` |
| `api.baseUrl` | `http://localhost:8081` |
| `admin.username` / `admin.password` | `admin` / `admin123` |
| `selenide.headless` | `true` |
| `selenide.browser` | `chrome` |

Переопределение: env `UI_BASE_URL`, `API_BASE_URL`, `ADMIN_*`, `SELENIDE_*` или `-Dapi.baseUrl=...`.

## Дерево

См. [source-tree-analysis.md](./source-tree-analysis.md) (часть `autotests/`).

## Разработка

```bash
# нужны поднятые backend :8081 и frontend :5173
cd autotests
mvn test
```

`./mvnw` скопирован из backend; на части машин wrapper зависит от `wget` — предпочтителен системный `mvn`.

## Тестирование (уровни в репо)

| Уровень | Где | Стек |
|---------|-----|------|
| Unit / IT кода | `backend/src/test` | JUnit + MockMvc + H2 |
| FE gate | `frontend/` | `npm run lint` + `npm run build` |
| E2E API/UI | `autotests/` | Rest Assured + Selenide |

## Интеграция со стендом

- API → `api.baseUrl` напрямую (не через Vite proxy)
- UI → `ui.baseUrl` в браузере (Chrome/headless)
- Auth smoke: `POST /api/auth/login`; UI smoke: форма `/login`
