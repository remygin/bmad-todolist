---
title: Adversarial architecture review — spine holes via compatible-but-clashing units
artifact: ARCHITECTURE-SPINE.md
reviewed: '2026-07-18'
method: construct two one-level-down units that each obey every AD letter-for-letter yet produce incompatible shared shapes, dual entity owners, or conflicting mutation paths
verdict: spine-incomplete
---

# Adversarial review: Architecture Spine

**Stance:** Assume two independent epic/story authors never talk. Each cites ADs and Consistency Conventions as green. If they can still ship incompatible substrates, the spine has a hole — close it with a new or tightened AD.

**Verdict:** **Spine-incomplete.** Layering, auth, Flyway, and HTTP path are constrained; aggregate ownership, Board JSON composition, lane identity (`status` vs `column.position`), Card write authority, and client post-mutation sync rules are not. Below: attack pairs, then findings, then proposed AD closures. Spine was **not** edited.

---

## Attack pairs (one level down)

Each pair: Unit α and Unit β. Both claim full AD compliance. Together they break.

### Pair 1 — Clashing Board shared-data shape (nested vs flat)

**Unit α — Epic: “Board detail as nested Kanban tree”**
- `GET /api/boards/{id}` returns `BoardResponse` with `columns[]`, each column nesting `cards[]` (current shape).
- FE `Board` / `BoardColumn.cards` mirrors that tree; local React state is one `Board`.
- Packages: `board/` owns response assembly; imports `CardDtos.CardResponse` (AD-1, AD-8).
- HTTP via `api/kanban.ts` + `apiRequest` (AD-9).

**Unit β — Epic: “Cards as first-class list under board”**
- `GET /api/boards/{id}` returns columns **without** nested cards; `GET /api/boards/{id}/cards` returns `CardResponse[]`.
- FE holds `{ board, columns, cards }` and groups by `status` in the page (AD-9: still local React state, no global store).
- Card list DTOs live in `card/*Dtos`; board DTOs stay in `board/*Dtos` (AD-8).
- Layering and auth unchanged (AD-1–AD-3, AD-5).

**Why both are “legal”:** AD-8 requires nested records in `*Dtos` and FE types beside API functions — **not** a single Board wire composition. Capability map splits boards vs cards by package but does not pin the GET-board payload. AD-1 allows Kanban behavior in `board` **or** `card`.

**Incompatibility:** α and β produce non-interoperable FE state and OpenAPI-shaped contracts; one epic’s components cannot consume the other’s board load without a rewrite.

**Hole → close:** Pin canonical Board read model (nested columns→cards) as the only board-detail contract; forbid alternate list endpoints that redefine the same screen’s data graph without an explicit AD change.

---

### Pair 2 — Two owners of Card (and CardResponse)

**Unit α — Story: “BoardService is the board aggregate root”**
- All Card reads for the board UI go through `BoardService.toResponse` / `toCardResponse`.
- Evolution of displayed card fields (e.g. add `columnId`, rename description nullability) is done in `board/` mapper.
- `CardService` remains write-only for create/update/move/delete (AD-2: service may use peer repos of “its” aggregate — α treats Card as inside Board).

**Unit β — Story: “card package owns the Card entity end-to-end”**
- `CardResponse` and all mappers live only in `card/`; `BoardService` must call `CardService.listByBoard` (service→service) or duplicate mapping “temporarily”.
- Schema/index changes for cards are proposed as Flyway from card-focused stories (AD-7 satisfied either way).
- AD-1: new Kanban behavior in `card` — β takes that literally for reads too.

**Why both are “legal”:** AD-2 allows peer repositories of “their aggregate” but **never defines the aggregate boundary**. ER seed shows Board→Card and Board→Column; packages are split. AD-5 scopes by `author_id` but does not name a single write/read owner service for Card. AD-8 allows `CardResponse` in `CardDtos` while `BoardDtos.ColumnResponse` embeds it — dual gravity.

**Incompatibility:** Parallel mappers drift (`BoardService.toCardResponse` vs `CardService.toResponse`); field additions land in one path only; FE sees different shapes from GET board vs POST/PATCH card.

**Hole → close:** Name Board as aggregate root for Kanban persistence reads; CardService is the **only** mutator of Card rows; single mapper for `CardResponse`; BoardService may read CardRepository but must not define a second response mapping.

---

### Pair 3 — Conflicting Card.position mutation paths

**Unit α — Story: “CardService normalizes positions on create/move/delete”**
- Exactly AD-6; locks board via `findOwned*ForUpdate` (AD-5).
- No other service touches `Card.position`.

**Unit β — Story: “Configure columns may compact / rebucket cards”**
- On `PUT .../columns`, after validating three statuses (AD-4), `BoardService` also rewrites card positions (e.g. “ensure contiguous positions per status”, or “archive visual reset”).
- Still author-scoped + pessimistic board lock (AD-5).
- Still “server owns position” (AD-6) — β argues normalization is server-side, not limited to CardService.
- AD-2: BoardService uses `CardRepository` as peer repo of the board aggregate.

**Why both are “legal”:** AD-6 lists normalize after create/move/delete — it does **not** say those are the **only** writers of `position`, nor that CardService is exclusive. AD-4 atomic column save does not forbid card side-effects.

**Incompatibility:** Concurrent α move and β column-save both lock the board but apply different normalize algorithms / ordering assumptions; FE optimistic move (AD-6 as-is) races with a board refresh that rewrote positions for unrelated reasons.

**Hole → close:** Sole mutation authority for `Card.position` (and `Card.status`) is CardService; BoardService column configure must not write Card rows. Optionally: column configure returns Board read model without triggering card rewrites.

---

### Pair 4 — Lane identity: `ColumnStatus` vs `column.position`

**Unit α — Story: “Visual lanes follow column.position (0..2)”**
- After FR-10 reorder, FE sorts `board.columns` by `position` for left-to-right lanes.
- Cards remain attached by matching `card.status === column.status` (as today).
- AD-4: same literals; JSON `name`; positions 0..2.

**Unit β — Story: “Lanes follow enum ordinal / fixed status order”**
- FE renders lanes in fixed order `TODO`, `IN_PROGRESS`, `DONE` regardless of `column.position`.
- Column settings UI still saves positions (AD-4 atomic full-set) but β treats `position` as metadata for settings form only, not for board chrome.
- DnD still sends `targetStatus` + `targetIndex` (AD-6).

**Why both are “legal”:** AD-4 forbids custom statuses and pins literals + display `name`; it does **not** say that `column.position` is the sole authority for on-board lane order vs status enum order. Cards hang on `status`, not `column.id` — spine never says whether UI lane order is position-driven or status-driven.

**Incompatibility:** Same API payload; after reorder, α and β show different boards. Stories that add keyboard DnD or a11y labels will wire to different orderings.

**Hole → close:** AD: on-board lane order **is** `column.position` ascending; status is membership key only; FE must not substitute enum declaration order for lane order.

---

### Pair 5 — Conflicting client state-mutation paths after success

**Unit α — Story: “Honor as-is move: optimistic apply, ignore move response body”**
- Implements AD-6 assumption literally for PATCH move.

**Unit β — Story: “Trust server Card on create/update; merge into local Board”**
- On POST create / PUT update, replace or insert from `CardResponse` (positions from server).
- Still no global store (AD-9); still may ignore move response (AD-6).
- After create+move sequence, local positions are a mix of server-authoritative (create) and client-authoritative (move).

**Why both are “legal”:** AD-6 constrains **move** response handling and reload truth; it is silent on create/update/delete/configureColumns response application. AD-9 only bans a second HTTP stack and global store.

**Incompatibility:** Divergent local truth machines; bugs only appear in multi-step flows; SM-2 “after reload” passes while in-session UX disagrees between features.

**Hole → close:** One client sync policy table: which mutations apply response body vs optimistic-only vs full board replace — at minimum for create, update, delete, move, configureColumns, rename board.

---

### Pair 6 — Lock target ambiguity (Board row vs Card row)

**Unit α — Story: “Pessimistic lock = board row (`findOwnedByIdForUpdate`)”**
- Current pattern; all card mutations take board lock.

**Unit β — Story: “Pessimistic lock = card row + status-scoped lists”**
- Adds `findOwnedCardForUpdate`; interprets AD-5 `findOwned*ForUpdate` as “whatever entity you mutate”.
- Column configure still locks board.

**Why both are “legal”:** AD-5 says mutations that reorder take `findOwned*ForUpdate` — wildcard `*` does not name Board as the lock grain. Two cards moving in different statuses could interleave under β and corrupt normalize under α’s assumptions.

**Incompatibility:** Mixed lock grains → lost updates on `position` under concurrency (SM-2 risk).

**Hole → close:** Reorder/normalize lock grain is always the **Board** row for any Card position/status mutation and for column configure.

---

### Pair 7 — Error envelope subset vs full body

**Unit α — Story: “FE ApiError = `{ message, details }` only”** (AD-8 text)
**Unit β — Story: “FE logs `timestamp`, `status`, `error`, `path` from body”** (matches actual `ErrorResponse` record; AD-8 does not forbid extras)

**Clash:** Typed clients and error UX diverge; AD-8 documents a subset while code emits a superset — spine does not say “additional fields allowed but non-normative” vs “exact shape”.

**Hole → close:** Normative error JSON = full handler body **or** explicitly “consumers must only rely on message+details”.

---

### Pair 8 — ColumnStatus type ownership

**Unit α:** Single `board.ColumnStatus`; card imports it.
**Unit β:** Introduce `card.CardStatus` with identical literals to “avoid depending on board package from card domain” (AD-1 package-by-domain; AD-4 only requires shared **literals**).

**Clash:** Dual enums, dual FE unions if someone mirrors β, serializer/config drift.

**Hole → close:** One canonical enum/type name and package (`ColumnStatus` in `board` or `common`); card and FE must import that single definition.

---

### Pair 9 — Active board selection state

**Unit α:** Active board id in `BoardsPage` local state (AD-9).
**Unit β:** Persist last active board id in `sessionStorage` (not forbidden — AD-3 only constrains the **token** key).

**Clash:** Two features disagree on cold-start active board (PRD FR-5 “first board becomes active” vs β restore).

**Hole → close:** AD or convention: active board is session UI state only in React; no `sessionStorage` keys except `todolist.accessToken` unless listed.

---

### Pair 10 — Health vs Kanban error semantics for FE

**Unit α:** Treat non-JSON `/actuator/health` separately; never parse via `apiRequest` Kanban path.
**Unit β:** Route health through same `apiRequest` helper “for AD-9 single path” and expect `ApiError` shape on failure.

**Clash:** AD-9 “every API call” vs AD-10 actuator — “API” underspecified; health is not `/api` and not the error envelope.

**Hole → close:** `apiRequest` is for `/api/**` only; actuator/health is out of band.

---

## Findings (holes)

1. Board detail wire composition (nested columns→cards vs flat/split endpoints) is unbound — Pair 1.
2. Card entity / `CardResponse` has two plausible package owners — Pair 2.
3. `Card.position` / `Card.status` writers are not exclusive to CardService — Pair 3.
4. Lane order authority (`column.position` vs status enum order) unset — Pair 4.
5. Client post-success sync policy only partially specified (move only) — Pair 5.
6. Pessimistic lock grain for reorder is ambiguous (`findOwned*`) — Pair 6.
7. Error JSON normative shape is subset of real `ErrorResponse` — Pair 7.
8. `ColumnStatus` single-type ownership not mandated — Pair 8.
9. `sessionStorage` reserved keys only named for JWT — Pair 9.
10. AD-9 “every API call” vs actuator/health out-of-band — Pair 10.
11. AD-2 “peer Repositories of their aggregate” without aggregate map — root cause of Pairs 2–3, 6.
12. Capability map lists modules but not **exclusive** write rights per entity — amplifies dual-owner attacks.
13. Structural seed ER shows relationships but no “aggregate root / consistency boundary” annotation.
14. AD-6 ASSUMPTION documents FE move as-is but leaves create/update/delete/configure sync to folklore.
15. Consistency table pins naming and trim/uniqueness but not shared DTO ownership across `BoardDtos` ↔ `CardDtos`.

---

## Proposed AD closures (do not apply here — review only)

| ID | Action | Draft rule |
| --- | --- | --- |
| **AD-11** (new) | Aggregate map | Board is the Kanban aggregate root. `BoardColumn` and `Card` are consistency-bound to one Board. Only `BoardService` performs Board/Column writes. Only `CardService` performs Card writes (`title`, `description`, `status`, `position`). Services may **read** peer repos; they must not write the other package’s entities. |
| **AD-4′** (tighten) | Lane identity | Membership of a card in a lane is `Card.status`. Left-to-right lane order on the board UI and in `BoardResponse.columns` is `BoardColumn.position` ascending (0..2). FE must not order lanes by enum declaration order. |
| **AD-5′** (tighten) | Lock grain | Any mutation that changes Card order/status or Column set takes pessimistic lock on the **Board** row (`findOwnedByIdForUpdate`). Do not use card-row locks as a substitute for board reorder safety. |
| **AD-6′** (tighten) | Sole position writer + client sync table | Server: only `CardService` create/move/delete normalize `Card.position`. Client: document per-operation whether to apply response, optimistic-only, or replace board from `BoardResponse` (configureColumns / getBoard). |
| **AD-8′** (tighten) | Board read model + errors + status type | Canonical board detail: `BoardResponse.columns[]` each with nested `cards[]` grouped by column status; no alternate split read for the same UI without spine change. Single `CardResponse` mapper. Normative error consumer fields vs full body. Single `ColumnStatus` type. |
| **AD-9′** (tighten) | Storage + HTTP scope | `apiRequest` only for `/api/**`. Only allowed `sessionStorage` key is `todolist.accessToken` unless a new AD lists another. |

---

## Severity rollup (for triage)

| Severity | Hole |
| --- | --- |
| **Critical** | No aggregate / exclusive write map (Pairs 2–3, 6; findings 2, 3, 6, 11) — two stories can both mutate Card under AD cover. |
| **Critical** | Board shared-data composition unbound (Pair 1; finding 1) — FE/BE contracts fork immediately. |
| **High** | Lane identity `position` vs `status` (Pair 4; finding 4) — same payload, different UX after FR-10. |
| **High** | Client mutation sync only specified for move (Pair 5; finding 5, 14) — in-session divergence. |
| **Medium** | Error envelope / ColumnStatus / sessionStorage / health path (Pairs 7–10) — drift and ops/FE mismatches. |

---

## Conclusion

The spine successfully blocks alternate frameworks, auth storage, custom statuses, and schema freelancing. It fails the adversary test on **shared Kanban data graph**, **entity write ownership**, and **state-mutation path exclusivity**. Until AD-11-style aggregate rules and tightened AD-4/5/6/8/9 land, two compliant epics can still ship an unmergeable substrate.
