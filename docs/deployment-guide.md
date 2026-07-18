# Руководство по развёртыванию

## Инфраструктура

| Сервис | Образ / сборка | Порт (host default) |
|--------|----------------|---------------------|
| db | `postgres:16-alpine` | 5432 |
| backend | `./backend` Dockerfile | 8080 |
| frontend | `./frontend` Dockerfile | 5173 → container 80 |

Volume: `pgdata` для PostgreSQL. Backend стартует после `service_healthy` у db.

## Полный стек

```bash
cp .env.example .env
# задайте сильный JWT_SECRET и ADMIN_PASSWORD
docker compose up --build
```

- Frontend: http://localhost:5173  
- Backend: http://localhost:8080  
- Health: http://localhost:8080/actuator/health  

## Backend образ

Multi-stage: `maven:3.9.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`, entrypoint `java -jar app.jar`.

Env (см. `.env.example` / compose):
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`
- `ADMIN_USERNAME`, `ADMIN_PASSWORD`

## Frontend образ

`node:20-alpine` build → `nginx:1.27-alpine`.  
`nginx.conf` проксирует `/api/` и `/actuator/health` на `http://backend:8080`, SPA fallback `try_files`.

## CI/CD

Пайплайнов в `.github/workflows` нет. Рекомендуется: `mvn test`, `npm run lint` + `npm run build` перед merge.

## Безопасность при деплое

- Сменить `JWT_SECRET` (≥ 256 бит для HS256) и пароль админа
- Не коммитить `.env`
- CORS сейчас `allowedOriginPatterns: *` — для production сузить origins
