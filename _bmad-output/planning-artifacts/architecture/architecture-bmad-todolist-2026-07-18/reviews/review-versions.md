# Version / reality-check review — Architecture Spine

**Spine:** `_bmad-output/planning-artifacts/architecture/architecture-bmad-todolist-2026-07-18/ARCHITECTURE-SPINE.md`  
**Date:** 2026-07-18  
**Scope:** Every Stack row + technology claims embedded in ADOPTED decisions (AD-1…AD-10), Consistency Conventions, and Deploy envelope — checked against repo files and (where suspicious) public release evidence.  
**Spine edited:** No.

## Verdict

**PASS — reality-checked against project files.** All pinned Stack versions match `pom.xml` / `package.json` / Dockerfiles / Compose / docs. Named technologies exist, still fit the as-is codebase, and were not invented from training data alone. Minor precision nits only (image tag suffix; one FE transitive dep omitted from Stack table).

## Method

| Source | Used for |
| --- | --- |
| `backend/pom.xml` | Java, Spring Boot, JJWT, Boot-managed JPA/Flyway/H2 |
| `frontend/package.json` (+ lock presence) | React, TS, Vite, RRD, @dnd-kit |
| `docker-compose.yml` | PostgreSQL image major |
| `backend/Dockerfile`, `frontend/Dockerfile` | Java/Node/nginx build images |
| `docs/architecture-*.md`, `docs/deployment-guide.md`, `docs/development-guide.md`, `docs/index.md` | Doc ↔ spine consistency |
| `application.yml`, `vite.config.ts`, `nginx.conf`, `frontend/src/api/client.ts` | AD-3 / AD-7 / AD-10 operational claims |
| Web (optional) | Spring Boot **4.0.7** existence (highest hallucination risk); JJWT 0.12.6 / nginx 1.27 spot-check |

## Stack table — row-by-row

| Spine claim | Project evidence | Public existence / fit | Result |
| --- | --- | --- | --- |
| Java **17** | `pom.xml` `<java.version>17</java.version>`; `eclipse-temurin:17-jre` / `maven:3.9.9-eclipse-temurin-17` | LTS; correct for Boot 4 | **OK** |
| Spring Boot **4.0.7** | `spring-boot-starter-parent` `4.0.7`; Boot 4-style starters (`webmvc`, `*-test` modules) | Released 2026-06-10 (spring.io / GitHub `v4.0.7` / Maven Central) | **OK** (web-verified) |
| JJWT **0.12.6** | `jjwt.version` + api/impl/jackson | Published artifact; matches Boot Security JWT usage | **OK** |
| Spring Data JPA + Flyway (Boot-managed) | starters `data-jpa`, `flyway`, `flyway-database-postgresql` | Fit layered REST + migrations (AD-7) | **OK** |
| PostgreSQL **16** | Compose `postgres:16-alpine`; docs say 16 | Official major 16 line | **OK** (see nit: `-alpine`) |
| H2 (test, Boot-managed) | `h2` runtime dep; `MODE=PostgreSQL` in `application.yml` test profile | Fits AD-7 | **OK** |
| React **18.3** | `react` / `react-dom` `^18.3.1` | Active; fits SPA paradigm | **OK** |
| TypeScript **~5.6** | `typescript` `~5.6.2` | Matches docs + spine | **OK** |
| Vite **5.4** | `vite` `^5.4.10` | Matches FE build / proxy story | **OK** |
| react-router-dom **6.30** | `^6.30.4` | Exists; fits pages routing | **OK** |
| @dnd-kit/core **^6.3** | `^6.3.1` | Fits Kanban DnD | **OK** |
| @dnd-kit/sortable **^10.0** | `^10.0.0` | Fits; used with core | **OK** |
| Node (build) **20+** | `FROM node:20-alpine` | Matches “20+” | **OK** |
| nginx (FE prod) **1.27** | `FROM nginx:1.27-alpine` | Official 1.27 line still published | **OK** |

### Stack omissions (non-blocking)

| Item in project, not in spine Stack | Severity | Note |
| --- | --- | --- |
| `@dnd-kit/utilities` `^3.2.2` | Low | Present in `package.json` and docs FE table; spine lists only core/sortable. Not a wrong version — incomplete inventory. |
| Maven **3.9.9** (build image) | Info | Build tooling only; not a runtime stack pin. |
| Exact Compose tag `postgres:16-alpine` | Low | Spine/AD-10 say `postgres:16`; major matches. |

## ADOPTED decisions — technology / version reality

| Decision | Tech claims checked | Evidence | Result |
| --- | --- | --- | --- |
| **AD-1** Layering; `jakarta.*`; no Lombok | Spring / Jakarta EE naming; package shape | Boot 4 + `jakarta` is the correct generation; structure matches docs | **OK** |
| **AD-2** FE→BE `/api` only | REST JSON boundary | Vite/nginx proxy to backend; no FE→DB | **OK** |
| **AD-3** JWT Bearer; `sessionStorage` `todolist.accessToken`; TTL via `JWT_EXPIRATION_MS` | JJWT + Security + FE client | `client.ts` TOKEN_KEY; yml `expiration-ms: ${JWT_EXPIRATION_MS:3600000}` (1h default) | **OK** |
| **AD-4** Three-column statuses | Domain invariant (not version) | Documented in data models / PRD bind | **OK** (non-version) |
| **AD-5** Ownership / locking | JPA patterns | Fits Spring Data JPA stack | **OK** |
| **AD-6** Server position authority | FE optimistic as-is | Assumption labeled; not a version claim | **OK** |
| **AD-7** Flyway-only; `ddl-auto=validate`; H2 `MODE=PostgreSQL` | Flyway + Hibernate + H2 | `application.yml` confirms both | **OK** |
| **AD-8** Error envelope / DTO records | Boot Validation + FE types | Fits current stack | **OK** |
| **AD-9** Single `apiRequest` path; no axios/RQ | fetch-only FE | `package.json` has no axios/react-query | **OK** |
| **AD-10** Compose `postgres:16` → backend → frontend nginx; dual ports 8081/8080; actuator health only | Images, ports, actuator | Compose + Dockerfiles; Vite→`8081`; Compose backend `8080`; `management.endpoints.web.exposure.include: health` | **OK** (postgres tag nit) |

## Docs cross-check

Docs (`architecture-backend.md`, `architecture-frontend.md`, `deployment-guide.md`, `development-guide.md`, `source-tree-analysis.md`, `index.md`) agree with the spine on Java 17, Boot 4.0.7, JJWT 0.12.6, PostgreSQL 16, React 18.3, TS ~5.6, Vite 5.4, RRD 6.30, nginx 1.27, Node 20. No contradictory version pins found.

## Findings

### Critical / High

_None._

### Medium

_None._

### Low

1. **AD-10 / Stack: `postgres:16` vs `postgres:16-alpine`** — Compose and deployment docs use the alpine tag. Major version is correct; tag string is slightly generalized.
2. **Stack omits `@dnd-kit/utilities`** — dependency is real in `package.json` and listed in `docs/architecture-frontend.md`; spine Stack is incomplete, not wrong.

### Informational

- Spring Boot **4.0.7** is real (released 2026-06-10) and matches the brownfield `pom.xml`; Boot 4 modular starters in the POM (`webmvc`, split test starters) corroborate that this is not a mislabeled Boot 3 project.
- FE versions in spine are **minor-line** pins; `package.json` uses caret/tilde ranges that resolve within those lines (`^18.3.1`, `^5.4.10`, `^6.30.4`, etc.) — appropriate for a brownfield spine.

## Conclusion

Committed Stack and deploy/runtime technology decisions in the architecture spine were **file-backed** (and, for Boot 4.0.7, **release-backed**). No invented frameworks, no version drift vs repo, no “downgrade Boot 4→3” hallucination trap. Accept spine versions as reality-checked; optional follow-ups are alpine tag wording and adding `@dnd-kit/utilities` to the Stack table (spine not modified in this review).
