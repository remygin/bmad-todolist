# BMAD: только конфигурация проекта

- Основная конфигурация BMAD находится в `./_bmad/`; локальные навыки и инструкции — в `./.<coding_agents>/skills/`.
- Не подменяй отсутствующие локальные файлы внешними версиями. Если нужной конфигурации или ресурса нет в проекте, остановись и явно сообщи об этом пользователю.

# Карта репозитория: код и тесты

| Каталог | Что пишется | Что НЕ пишется |
|---------|-------------|----------------|
| `backend/` | Код API (Spring Boot): controllers, services, JPA, Flyway, security | E2E / браузерные / Rest Assured против живого стенда |
| `backend/src/test/` | Unit и интеграционные тесты кода: JUnit 5 + MockMvc + H2 (`@ActiveProfiles("test")`) | Selenide, Playwright, тесты против поднятого UI/API |
| `frontend/` | Код UI (React/TS/Vite): pages, components, `src/api/*` | Автотесты раннера (Jest/Vitest/Playwright) без отдельной задачи |
| `frontend/` (проверка) | Gate качества: `npm run lint` + `npm run build` | Unit/E2E в этом каталоге по умолчанию |
| `autotests/` | E2E / системные автотесты против поднятого стенда: API (Rest Assured) и UI (Selenide) | Прод-код приложения; MockMvc / in-process Spring-тесты |

Кратко:

- **Код приложения** → `backend/` + `frontend/`
- **Тесты кода (unit/IT)** → только `backend/src/test/`
- **Автотесты E2E (API + UI)** → только `autotests/`
- Детали стека и инвариантов — в `_bmad-output/project-context.md` и `docs/`

# Сабмодули

`backend/`, `frontend/`, `autotests/` — отдельные git-репозитории, подключённые сабмодулями. Изменения внутри каталога коммитятся и пушатся в его собственный репозиторий; в монорепо затем коммитится обновлённый указатель сабмодуля (`git add <каталог>`).
