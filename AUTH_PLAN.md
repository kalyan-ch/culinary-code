# Auth — Password + Google Sign-In

Status: **partially implemented, paused for this plan** (2026-08-19).

Decided already: accounts live in this app (Spring Security, self-built, BCrypt, session
cookie), and recipes are **private by default with an opt-in publish**.

Decisions 1, 3 and 4 are settled (see the table at the end); only the account-model shape
(decision 2) is still open, and it does not block password auth.

---

## Where the code currently stands

Written, **not yet compiled or tested**:

| File | State |
|---|---|
| `build.gradle` | `spring-boot-starter-security` + `spring-security-test` added |
| `db/migration/V20260819120000__add_recipe_visibility.sql` | `is_public` column + partial index |
| `dao/RecipeUserRepository.java` | done |
| `security/AuthUser.java` | `UserDetails` record — **still assumes a password exists** |
| `model/auth/AuthDTOs.java` | register / login / user records |
| `config/SecurityConfig.java` | filter chain, BCrypt, 401 entry point, logout |
| `service/AuthService.java` | register + programmatic session login |
| `controller/auth/AuthController.java` | register / login / me |
| ~~`controller/auth/UserAuthenticationController.java`~~ | deleted — was the static-string stub |

Not started: recipe ownership + visibility enforcement, `GlobalExceptionHandler` cases,
`SameSite` config, the entire frontend.

**Nothing is committed.** Recipe endpoints are still unauthenticated and still trust
`userId` from the request body.

---

## What OAuth changes

Adding Google is genuinely small in Spring — `spring-boot-starter-oauth2-client`, a provider
registration in config, and `oauth2Login()` on the filter chain. Google is an OIDC provider
Spring knows natively, so there's no hand-rolled token exchange.

The work isn't the protocol. It's these four things:

### 1. The account model

An OAuth user still needs a `users` row, because `recipes.user_id` is a real foreign key.
The schema already anticipated this — `password_hash` is nullable, commented *"null if using
OAuth-only."*

Two columns carry it:

```sql
ALTER TABLE users ADD COLUMN provider     TEXT;  -- null for password accounts
ALTER TABLE users ADD COLUMN provider_id  TEXT;  -- the provider's stable subject id
CREATE UNIQUE INDEX idx_users_provider ON users(provider, provider_id)
    WHERE provider IS NOT NULL;
```

Match on `(provider, provider_id)`, never on email alone — a provider's email can change,
its subject id can't.

This allows one OAuth provider per account, plus an optional password. Supporting *several*
providers on one account needs a `user_identities` table instead; that's a real design, but
it's speculative until someone asks for it, and the migration from columns to a table is
mechanical.

### 2. Email collision

**Google → existing password account:** auto-link on a verified email. Google asserts
`email_verified: true`, so attach the provider to the existing row and sign in. Safe *only*
because Google verifies — never do this for a provider that doesn't, since the failure mode is
full account takeover.

The link is **never silent.** The OAuth success handler redirects to the frontend with a
marker (`/?linked=google`) and the UI shows a notice: *"We've connected your Google account to
your existing Culinary Code account."* Two reasons — someone who didn't realise they already
had an account needs to understand why their old recipes just appeared, and an account change
the owner didn't initiate should always be visible to them.

Follow-up once email delivery exists: also send *"Google sign-in was added to your account."*
The in-app notice tells whoever is holding the browser; the email tells the actual account
owner, which is the case that matters if the link was not them.

**Registration → email already taken:** respond "You already have an account — sign in",
and send them to the login form with the email pre-filled.

> **Rejected: signing them in when the submitted password happens to match.**
> This was proposed for convenience — people reuse passwords, so the one they type at
> registration is often the right one. The problem is that it turns `/register` into an
> authentication endpoint that bypasses every protection on `/login`: rate limiting, lockout,
> alerting. Anyone holding email/password pairs from an unrelated breach can test them here —
> a hit signs them straight into the account, a miss just errors. Password reuse, the reason
> the shortcut works, is precisely what makes it exploitable.
>
> Pre-filling the email on the login form gets the same outcome for a real user — one extra
> click — while keeping the credential check on the endpoint that is defended.

**Account recovery:** every account should end up with a password, including ones created via
Google, so recovery doesn't depend on the provider. Offer it **after** first sign-in or in
settings rather than mid-signup — a password form appended to "Continue with Google" is where
people abandon onboarding. An emailed reset link covers both account types and is worth having
regardless.

### 3. The reverse case

A Google-only account has no `password_hash`. If that person tries the password form, they
must get *"This account uses Google sign-in"* — not "bad credentials", and definitely not a
`NullPointerException` from BCrypt comparing against null. `AuthUser` as currently written
assumes a password exists; that assumption has to go.

### 4. The redirect flow forces the proxy

OAuth is a browser redirect: frontend → `/oauth2/authorization/google` → Google → callback →
backend sets the session cookie → redirect back to the frontend.

With Next on Vercel and Spring on Railway those are different registrable domains, so the
callback sets a **third-party cookie** — which Safari blocks outright. The fix is the same one
already planned for password sessions: proxy `/api/*` through Next `rewrites` so the browser
only ever talks to the Vercel origin. OAuth turns that from a good idea into a hard
requirement, and the Google redirect URI must be registered against the *proxied* path.

---

## Alternative worth naming: move auth to the frontend

Auth.js (NextAuth) in the Next app would handle Google, Apple and GitHub with better
ergonomics than Spring, issue its own session, and leave Spring validating a signed token.

Not recommended here — it means two auth systems during migration, or reworking the Spring
side into stateless token validation, and it moves identity out of the app that owns the data.
Worth reconsidering only if provider support becomes the dominant requirement.

---

## Provider scope

Google only, for now. GitHub later would be config-only — one more registration block, no new
code — but it would also be the point at which the `provider`/`provider_id` columns need to
become a `user_identities` table, since one row can hold only one provider.

Apple is deliberately excluded for now: it needs a paid developer account, and its client
secret is a JWT that has to be regenerated every six months, which is an operational chore
rather than a coding task.

---

## Shared vs. personal data

Reference data is global and deliberately not tied to an account; only recipes and the
planning data built on them belong to someone.

| Global | Personal |
|---|---|
| `ingredients` (name, category, default_unit) | `recipes` — `user_id` |
| `tags` (name) | `recipe_ingredients`, `recipe_steps`, `recipe_tags` — via the recipe |
| cuisines — *not a table*; `CUISINES` in the UI | `meal_plans` + entries — `user_id` |
| units — *not a table*; `UNIT_OPTIONS` in `IngredientTable.tsx` | `grocery_lists` + items + sources — `user_id` |
| `difficulty_level`, `recipe_source_type` — PG enums | |

The schema already had this right: only `recipes`, `meal_plans` and `grocery_lists` carry a
`user_id`.

### Curated vs. invented reference data

`tags` and `ingredients` each carry a nullable `user_id`: **NULL means curated** — a shared row everyone uses — and
a populated one means the user invented it. `recipe_tags` and `Recipe.tags` are unchanged; a
separate `user_tags` table would have meant a second join table, a second entity, and merge
logic in every read for the same semantics.

Two partial unique indexes enforce it: `UNIQUE(lower(name)) WHERE user_id IS NULL` so curated
names can't collide, and `UNIQUE(user_id, lower(name)) WHERE user_id IS NOT NULL` so two users
may each keep a tag of the same name. `resolveTag` checks curated first, then the caller's own,
and otherwise creates one **owned by the caller** — nothing in the service can write a curated
row. That also fixes the earlier leak on `GET /api/v1/tags`, which now returns the curated set
plus the viewer's own.

`resolveIngredient` follows the same rule as `resolveTag`, so an unrecognised name is created
against the user rather than added to the shared catalogue. Curated ingredients keep their
`category` and `default_unit`; invented ones have neither, which means they land in an
"uncategorised" bucket when aisle grouping is built.

`V20260819130000__reference_data.sql` seeds **47 curated tags and 288 curated ingredients**,
covering Indian, West African, Caribbean, East and South-East Asian, Mediterranean and
European cooking rather than only Anglo staples. It is a regular migration, not local-only
sample data: an empty catalogue makes grocery-list merging and aisle grouping useless in
every environment.

Still open:

- **No ingredient listing endpoint exists yet.** Autocomplete will need the same scoping as
  `GET /api/v1/tags` — curated plus the viewer's own.
- **Invented rows carry no category**, so a future grocery list cannot aisle-group them.
  Promoting a frequently-used invented ingredient into the curated set is a manual job for
  now, which is fine until it isn't.

---

## Build order

1. ✅ Password auth compiling — `SecurityConfig`, BCrypt, session cookie.
2. ✅ `AuthController` — register, login, `/me`; logout via the filter chain.
3. ✅ Ownership + visibility: owner from the session, `userId` dropped from the request,
   owner-or-published on read, owner-only on write, `mine` filter on the list.
4. ✅ `GlobalExceptionHandler`: 401, 403 and `ResponseStatusException` in the app's error shape.
5. ✅ `V20260819140000__add_oauth_provider.sql` — `provider` / `provider_id`, a partial unique
   index, and CHECK constraints so an account always has either a password or a provider, and
   never half a provider pair.
6. ✅ `oauth2Login()` under `/api/oauth2/**` so the frontend proxy covers the redirect;
   `OAuthLoginService` resolves provider-id → verified-email link → new account, and
   `OAuthSuccessHandler` swaps the OIDC principal for `AuthUser` so controllers see one type.
7. ⏳ Frontend — in progress.

**Still needed from the operator before Google sign-in works:** `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET` in the environment (the config uses harmless placeholders so the app
boots without them), and the redirect URI registered in the Google console as
`{frontend-origin}/api/login/oauth2/code/google`. `server.forward-headers-strategy: framework`
is set so the callback builds that URL from the proxy's forwarded host rather than the
backend's own.

---

## Decisions

| # | Decision | Status |
|---|---|---|
| 1 | Auth stays in Spring, or moves to Auth.js in Next | **settled** — Spring |
| 2 | `provider`/`provider_id` columns on `users`, or a `user_identities` table | **settled** — columns; Google is the only provider for now |
| 3 | Email collision handling | **settled** — auto-link on verified email, and tell the user; never silent |
| 4 | Password for Google accounts | **settled** — yes, but offered after first sign-in, not during signup |

---

## Note for later

The shelved [local-first downloadable app](LOCAL_FIRST_PLAN.md) can't use this browser
redirect flow directly — a desktop or mobile client needs PKCE against the system browser.
Not a blocker, and not a reason to decide anything differently now.
