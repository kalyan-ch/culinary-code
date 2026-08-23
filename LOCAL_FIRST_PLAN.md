# Local-First + Sync — Architecture Plan

Status: **shelved, kept deliberately** (2026-08-19). Not the current direction.

The hosted app is going the other way: real accounts, each user managing their own recipes
(see the auth work). This document describes a **second, alternative route** to be built
later — a downloadable app (phone, desktop) for users who would rather keep their recipes on
their own device than in an account. The two can coexist: an account holder syncs through the
server; a local-only user never creates an account at all.

One consequence of that reframing, worth noting before this is ever picked up: the plan below
was written for a single person syncing their own devices. As a shipped product feature it
gains a per-user dimension — the sync endpoint becomes authenticated, and `Phase 6`'s
"library code" problem is simply solved by the account the user already has. That makes the
downloadable app *easier* to build after auth exists, not harder.

Original goal (unchanged below): the app works fully offline on any device, and recipes
created on one device appear on the others once they're back online.

---

## The tension this plan resolves

Three of the requirements pull against each other:

1. **No accounts** — nothing to sign up for, no credentials anywhere.
2. **Works offline on any device** — data has to live *on* the device.
3. **Shared across devices, from anywhere** — something reachable from the open internet
   has to hold the shared copy.

(2) and (3) together mean two copies of the data that can both be edited independently, which
is the definition of a sync problem. (1) and (3) together mean an internet-reachable server
with no login — so something other than a password has to stop strangers from reading and
deleting your recipes. Both are solvable; neither is free. See **Phase 6** for the second one.

---

## Core design decisions

### The recipe is the sync unit, not the row

A recipe plus its ingredients, steps and tags syncs as **one document**. Individual
`recipe_ingredients` / `recipe_steps` rows are never synced independently.

Why: `RecipeService.updateRecipe` already does a full replace of those collections rather than
a diff, so the aggregate is already the real unit of change in this codebase. Ingredients and
steps have no identity worth preserving across an edit — nobody cares that step 3 kept its
UUID. Syncing whole documents collapses the conflict surface from "hundreds of rows" to
"one row per recipe", which is the single biggest simplification available here.

### Last-write-wins per document, not CRDTs

Automerge/Yjs solve concurrent edits to the *same* document by multiple people. This is a
personal recipe app: one person, editing one recipe at a time, on one device at a time.
Paying a CRDT's cost — a rewritten data model, a large dependency, opaque debugging — buys
almost nothing here. Per-document LWW with an explicit conflict rule (below) is the right
tool.

### Conflicts fork, they never silently overwrite

Each push carries the `seq` the client last saw for that recipe (`baseSeq`).

| Server state | Outcome |
|---|---|
| `seq == baseSeq` | Clean write. Server bumps `seq`, returns it. |
| `seq > baseSeq` | **Conflict.** Server keeps its version; the client's version is written as a *new* recipe with a new id, titled `"<title> (edited on iPhone)"`. |
| Row tombstoned, client edited it | Same fork — the client's edit comes back as a new recipe. |

The user resolves a conflict by deleting the copy they don't want. This is the Dropbox
approach and it has one enormous advantage over silent LWW: **it can't lose an edit you
typed.** Silent LWW on a recipe you spent ten minutes writing is a genuinely bad outcome, and
it's invisible when it happens.

### Client-generated UUIDs

Offline creates need an id before the server has ever seen them. `crypto.randomUUID()` in the
browser, no dependency. The schema already uses UUID primary keys, so this is a small change.

**Backend gotcha:** `Recipe.id` is currently `@GeneratedValue(strategy = GenerationType.UUID)`,
which makes Hibernate assign the id and ignore a client-supplied one. Dropping `@GeneratedValue`
fixes that, but then Spring Data sees a non-null id on `save()`, assumes the entity already
exists, and issues a `SELECT` before the `INSERT`. Either implement `Persistable<UUID>` with a
transient `isNew` flag, or accept the extra select — at this scale it does not matter.

### Deletes are tombstones

`deleted_at TIMESTAMPTZ` on `recipes`. `DELETE /api/v1/recipe/{id}` sets it instead of removing
the row; every read filters `deleted_at IS NULL`. A hard delete can't propagate — the other
device has no way to tell "deleted" from "not yet synced." Purge tombstones older than ~90 days
with a scheduled job.

### The sync cursor is a sequence, not a timestamp

```sql
CREATE SEQUENCE change_seq;
ALTER TABLE recipes ADD COLUMN seq BIGINT;
```

Set `NEW.seq = nextval('change_seq')` on insert and update — the existing `set_updated_at()`
trigger from `V20260816221000` is the natural place to extend. The client stores the highest
`seq` it has seen; pulling is `WHERE seq > :cursor ORDER BY seq`.

Timestamps would work until a device's clock drifted, and then they'd fail silently and
skip records. A sequence has no clock in it.

> **Known caveat:** with concurrent writers, a transaction that reserved a lower `seq` can
> commit *after* one with a higher `seq`, so a client that advances its cursor past the higher
> value can miss the lower one. With one user this is close to impossible, and the fix if it
> ever matters is to re-pull a small overlap window rather than trusting the cursor exactly.

### One endpoint replaces most of the REST API

```
POST /api/v1/sync
  { "cursor": 1234, "changes": [ { ...recipe doc, "baseSeq": 1201 } ] }
→ { "cursor": 1290,
    "changes":   [ ...recipe docs changed since 1234... ],
    "conflicts": [ { "clientId": "...", "forkedAs": "..." } ],
    "hasMore":   false }
```

Push and pull in one round trip. `hasMore` pages the first sync on a fresh device.

Worth noting what this **deletes**: once the client holds the whole library locally, the
paginated list endpoint, `RecipeSpecifications`, and `GET /api/v1/tags` all become dead code.
Filtering, pagination and tag listing become local queries. Title search — the `pg_trgm` index
that's been sitting unused in the schema — becomes a client-side string match for free.
The API shrinks to sync plus, eventually, image blobs.

---

## Phases

Ordered by dependency. Each is independently useful and leaves the app working.

### Phase 0 — Remove users

Independent of everything else, no risk, unblocks the rest.

- **Migration:** drop `users`; drop `recipes.user_id`, `meal_plans.user_id`,
  `grocery_lists.user_id` and their indexes and FKs; drop the users seed insert.
- **Backend:** delete `RecipeUser`, `Recipe.userId`, `@NotNull userId` on
  `RecipeCreateRequest`, `RecipeSpecifications.hasUserId`, the `userId` list filter, the
  `UserAuthenticationController` `/login` stub, and `trg_users_updated_at`.
- **UI:** delete `PLACEHOLDER_USER_ID` (`src/lib/constants.ts`), its use at
  `CreateRecipeForm.tsx:103`, and `userId` from `types.ts` (3 places).

### Phase 1 — Sync-ready schema

- `change_seq` sequence; `seq BIGINT` on `recipes`; extend `set_updated_at()` to set it.
- `deleted_at` on `recipes`; convert delete to soft delete; filter every read.
- Drop `@GeneratedValue` from `Recipe.id`; accept client-supplied ids on create.
- Index on `(seq)` for the pull query.

### Phase 2 — Sync endpoint

- `SyncController` + `SyncService`: apply pushes with the fork rule, return the delta.
- Reuse the existing whole-document rebuild logic from `updateRecipe` — the ingredient/tag
  resolve-or-create caches and the `saveAndFlush`-before-re-add ordering fix already handle
  the hard parts.
- Tests: clean write, conflicting write forks, tombstone propagation, cursor paging.

### Phase 3 — Local store + client-side pages

This is where the UI actually changes shape.

- **Dexie** (IndexedDB) with tables `recipes` (the documents), `meta` (cursor, device name).
  A `dirty` boolean plus `baseSeq` on each recipe is enough to track pending pushes — no
  separate outbox table, and no outbox ordering problems.
- **Every data page becomes a client component.** `/search`, `/recipe/[id]`,
  `/recipe/[id]/edit` and `Sidebar` are all async Server Components fetching from the API
  during SSR today. IndexedDB does not exist on the server, so all four must read locally
  via Dexie's `useLiveQuery` instead.
- Upside: `useLiveQuery` is reactive, so a background sync landing new data re-renders the
  open page automatically. No refetch plumbing.

### Phase 4 — Sync engine

- Trigger on: app open, `online` event, after any local write (debounced), periodic timer.
- Push all `dirty` recipes, apply the returned delta, advance the cursor, clear `dirty`.
- Retry with backoff; never block the UI on it. Surface a small status ("Synced 2m ago",
  "Offline — 3 changes pending") so the state is never a mystery.

### Phase 5 — PWA shell

- `output: 'export'` in `next.config.ts` — the whole UI becomes a static bundle, which is
  what makes it cacheable and installable. (Requires Phase 3 to be done: no server components.)
- **Serwist** for the service worker (`next-pwa` is unmaintained for Next 15 App Router).
  Cache the app shell; the data layer is already local so the SW only handles static assets.
- `manifest.json`, icons, `display: standalone` — installs to the home screen on iOS/Android.
- Optionally serve the static build from Spring Boot's `src/main/resources/static`, giving
  one deployable artifact on one port.

### Phase 6 — Reachability, and the no-accounts problem

This is the open decision. **The sync work above is identical either way** — only the
deployment and one scoping column differ — so it does not block Phases 0–5.

**Option A — Tailscale, server stays unauthenticated.** The network is the security boundary.
Genuinely zero accounts, nothing exposed publicly. Cost: Tailscale must be installed on every
device, so "any device" becomes "any device of mine." A friend's iPad can't open it.

**Option B — public host, library code.** The app generates a random `library_id` on first
launch; a second device joins by scanning a QR code from the first. The server scopes all data
by that id, which doubles as the bearer token. No email, no password, no registration — so it
satisfies "no account creation" — but be clear-eyed about what it is: **a `library_id` column
is `user_id` under a different name.** What changes is the experience (nothing to sign up for,
nothing to remember, pair by QR) and the failure mode (lose the code on all devices and the
data is unreachable — so it's worth writing down).

Given "any device" and "away from home regularly," **B fits the stated requirements better**;
A is the more private option if you're willing to install Tailscale everywhere.

---

## What this costs

Worth having in view before starting:

- **Server Components go away for data pages,** and loading states come back with them. The
  Recipe CRUD phase deliberately avoided both. Phase 3 undoes that.
- **Sync bugs are the worst class of bug to reproduce.** They need two devices, airplane mode,
  and a specific interleaving. Budget for a test harness that drives two simulated clients
  against one server — trying to debug this by hand on a phone will be miserable.
- **Meal plans and grocery lists must be designed sync-aware from day one.** Good news: they're
  entirely unbuilt, so doing this *now* is the right order. A grocery list with items checked
  off on two phones at once is a much harder merge than a recipe — genuinely per-item state,
  and the one place where LWW-per-document is the wrong call.
- **Uploaded images would need blob sync.** `image_url` pointing at a remote URL is fine today
  and works offline only if cached. Real image upload means syncing binaries through IndexedDB,
  which is a separate project. Defer it.

## Open decisions

1. **Phase 6: Tailscale or library code.** Deferrable — nothing before Phase 6 depends on it.
2. **Grocery list merge semantics**, once that feature is designed. Per-item, not per-document.
