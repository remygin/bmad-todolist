# Architecture Spine Rubric Review — BMAD Todo List

**Lens:** good-spine checklist (`references/reviewer-gate.md`)  
**Target:** `ARCHITECTURE-SPINE.md`  
**Context:** brownfield as-is · altitude `initiative` · purpose `build-substrate` · sources code + PRD  
**Date:** 2026-07-18

## Overall verdict

Spine **adequate with gaps**: domain, layering, auth token storage, three-column invariant, ownership locks, Flyway, error/DTO shape, FE HTTP path, and Compose envelope correctly ratify the codebase and cover FR-1…FR-15. Two security/session boundary items that already exist in code and `project-context.md` are either silent or only soft-Deferred, so parallel feature units can still diverge. No critical contradictions with brownfield; not yet strong enough to call divergence coverage complete for initiative altitude.

**Severity counts:** critical 0 · high 2 · medium 2 · low 2.

---

## 1. Divergence coverage for the level below — adequate

Initiative altitude must freeze what features would otherwise invent differently. Covered well: Controller→Service→Repository vs pages/components/api (AD-1/AD-2), JWT Bearer + `sessionStorage` key + `hasRole('ADMIN')` (AD-3), fixed three statuses (AD-4), author-scope + pessimistic reorder + 404-not-403 (AD-5), server-owned `position` + move payload (AD-6), Flyway-only schema (AD-7), error envelope + field maxima (AD-8), single `apiRequest` path (AD-9), Compose/ports/actuator (AD-10).

Missed divergence points that features can still disagree on without violating a written Rule: CSRF/session policy and CORS interim freeze (see findings below). Style items (tabs) are not feature-divergence risks in the same sense.

### Findings
- **high** CSRF and session policy not bound (`AD-3`) — Code disables CSRF and uses `SessionCreationPolicy.STATELESS`; `project-context.md` explicitly forbids “improving” CSRF/session without a task. Spine AD-3 Prevents/Rule lock token storage and `hasRole`, but not CSRF-off / STATELESS. Two features can re-enable CSRF or change session mode and both claim compliance. *Action:* autofix — extend AD-3 Rule (and Prevents) to ratify `csrf disabled` + `STATELESS` until an explicit security task; or add a Deferred freeze line that forbids change without a story.
- **high** CORS left only in Deferred without interim freeze (`Deferred`, missing from AD-3/AD-10) — Deferred says narrowing from `*` is acceptable to revisit later, but no ADOPTED interim Rule says “keep `allowedOriginPatterns: *` / credentials false.” Feature A can tighten origins while Feature B assumes `*`. That is exactly “Deferred that lets two units diverge.” *Action:* autofix — either promote current CORS to AD-3/AD-10 as adopted as-is, or rewrite Deferred to an explicit freeze (“do not change CORS without ops/security story”).

---

## 2. AD Rule ↔ Prevents enforceability — adequate

Most Rules are observable and map to Prevents: layering paths, FE→BE-only dependency, Bearer/`sessionStorage`/`hasRole`, three statuses, ownership queries + 404, server `position`, Flyway + `ddl-auto=validate`, error JSON shape, `apiRequest`-only, Compose topology.

Weak spots: AD-1 packs style constraints that do not prevent the stated divergences; AD-6 correctly encodes as-is optimistic FE with an `[ASSUMPTION]` and aligns with PRD over stale project-context — enforceable for builders who read the spine.

### Findings
- **medium** AD-1 Rule mixes architecture with style (`AD-1`) — Prevents: business rules in controllers/UI, god-packages, parallel styles. Rule also mandates no Lombok, `jakarta.*`, and tabs. Those style clauses do not enforce the Prevents and dilute Rule testability; they already belong (or should live) under Consistency Conventions / project-context. *Action:* autofix — move Lombok/`jakarta`/tabs out of AD-1 Rule into Consistency Conventions; keep AD-1 Rule to package/layer placement only.

---

## 3. Deferred safety — weak on security envelope

Deferred items that are safe: CI/CD (absent), server revoke / mid-session 401 handler (product as-is), FE sync-from-move-response (already bound opposite by AD-6 + PRD), Non-Goals multi-user/custom statuses/export, cloud beyond Compose.

Unsafe as currently written: CORS (above). CSRF is worse — not even Deferred, just silent.

Optimistic-move Deferred correctly points at an explicit story and does not reopen AD-6.

### Findings
*(CORS finding listed in §1; no additional Deferred-only finding.)*

---

## 4. Named tech verified-current — strong (brownfield pin)

Stack versions match repo pins: Java 17, Spring Boot **4.0.7** (`pom.xml`), JJWT 0.12.6, PostgreSQL 16, React 18.3, TypeScript ~5.6, Vite 5.4, react-router-dom 6.30, @dnd-kit core/sortable, Node 20+, nginx 1.27. Versions are real published lines.

For brownfield build-substrate, “verified-current” means ratified as-is pins that exist, not forced upgrades. Ecosystem later lines (Spring Boot 4.1.0, React 19.x, Vite 8.x) exist as of 2026-07; spine correctly does not invent an upgrade path.

### Findings
- **low** Stack is as-is, not ecosystem-latest (`Stack`) — FE (React 18 / Vite 5) and Boot 4.0.x trail latest majors/minors. Acceptable for ratification; only a problem if a reader treats Stack as “upgrade target.” *Action:* ignore for finalize, or one-line note in memlog that pins are code-truth not greenfield seed.

---

## 5. Brownfield ratification — strong

Spot-check against code: `todolist.accessToken` in `sessionStorage`; `/api/**` authenticated + login/health public; `@PreAuthorize("hasRole('ADMIN')")`; `findByIdAndAuthorId` / `findOwned*ForUpdate`; Flyway + `ddl-auto=validate`; error envelope via handler; FE `apiRequest`; Vite→8081 vs Compose→8080; actuator health-only; three `ColumnStatus` literals; move `targetStatus`/`targetIndex`; optimistic move without applying response (`BoardsPage`).

AD-6 + Deferred correctly resolve PRD vs `project-context.md` conflict in favor of code/PRD. No parent spine; inheritance N/A.

### Findings
*(нет contradiction findings)*

---

## 6. Spec capability coverage — strong

Frontmatter `binds` FR-1…FR-15. Capability map covers auth (FR-1–4), boards (FR-5–8), columns (FR-9–10), cards (FR-11–14), health (FR-15), errors. Non-Goals appear under Deferred. SM-1/SM-2/SM-3/SM-C1 concerns are carried by AD-4/AD-5/AD-6.

Minor: board capability row omits AD-1/AD-9 but those bind globally — not a coverage hole.

### Findings
- **low** Health success wire shape not pinned (`AD-10` vs FR-15) — FR-15 requires HTTP 200 and `status: "UP"` with no component details; AD-10 pins exposure of health-only and env/ports, not the success body. Unlikely to fork features, but incomplete vs driving PRD consequence. *Action:* autofix — one clause on AD-10 Rule; or defer explicitly as “actuator default body.”

---

## 7. Parent inheritance — N/A

No parent spine; no Inherited Invariants section (correctly omitted).

---

## 8. Altitude dimension completeness — adequate

Initiative owns full-system envelope. Decided: paradigm, layering, authn/z token path, domain invariants, persistence ownership, API/FE contracts, Compose deploy + secrets + ports (AD-10). Deferred: CI/CD, CORS narrowing, revoke/401 handler, move-response sync story, Non-Goals, cloud providers. No Open Questions section — acceptable if nothing is left silent.

Gaps: CSRF/session (silent), CORS (Deferred without freeze), Structural Seed shows source tree + ERD but no deployment topology diagram though AD-10 owns that dimension in prose.

### Findings
- **medium** Structural Seed omits deploy topology (`Structural Seed` / AD-10) — Template calls for deployment & environments shape when this altitude owns the operational envelope. AD-10 decides Compose/postgres→backend→nginx and port split, but Seed has no container/proxy diagram; cold-start readers get ops only from AD prose. *Action:* autofix — add a small mermaid deploy diagram (db / backend:8080 / frontend nginx / Vite proxy note); or explicitly Deferred “diagram optional, AD-10 is authority.”

---

## Mechanical / checklist rollup

| Checklist item | Result |
| --- | --- |
| Fixes real divergence points; misses none | **Partial** — domain strong; CSRF + CORS interim incomplete |
| Every AD Rule enforceable & prevents stated divergence | **Mostly** — AD-1 Rule/Prevents mismatch on style |
| Nothing under Deferred lets units diverge | **Fail on CORS** — soft Deferred, no freeze |
| Named tech verified-current | **Pass** (brownfield pins verified real + match code) |
| Ratifies brownfield; does not contradict | **Pass** |
| Covers driving spec capabilities | **Pass** (FR-1…15 + Non-Goals) |
| Inherited ADs not weakened | **N/A** |
| Every owned dimension decided / deferred / open | **Partial** — CSRF silent; deploy diagram thin |

**Suggested gate disposition:** discuss/autofix the two **high** security-boundary items before `status: final`; mediums are clear autofixes; lows optional.

---

## Tail

Plus 2 low findings in this file (Stack as-is vs ecosystem-latest; health UP body not in AD-10).
