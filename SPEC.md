# SPEC — backend internals

## Architecture (decisions + why)

- **JOOQ DB-first, no ORM**: the Liquibase SQL schema is the source of truth and codegen from a live DB guarantees code/schema lockstep at compile time. JPA was rejected — triggers, JSONB metadata, PG native enums and hand-tuned queries don't fit entity mapping.
- **Joda-Time in the domain** (`jackson-datatype-joda` registered): historical choice kept for consistency; `DateTimeHelper` is the single crossing point to `java.time` and uses the system zone — timestamp conversion is timezone-sensitive.
- **Firebase behind an optional Lambda proxy**: `firebase.lambda.name` set → token checks go through `lambda/firebase-proxy`; blank → in-process Admin SDK. Both paths must stay behaviorally identical. The reason is **network, not instance size**: App Runner runs with `EgressType: VPC`, so the container has no route to the internet and cannot reach Google — the alternative would be a NAT Gateway (~$32/mo). The Lambda uses **SnapStart on the `live` alias**, so `firebase.lambda.name` must stay **qualified** (`ipredencao-firebase-proxy:live`); `$LATEST` gets no snapshot. Consequences: deploy only via `lambda/firebase-proxy/deploy.sh`, the single entry point (SAM publishes a version and moves the alias; a bare `update-function-code` touches only `$LATEST`, leaving prod silently on the old code without a snapshot), and the invoke policy must cover the qualified ARN. The invoking client sets explicit timeouts (`clientExecutionTimeout` 12s) so a slow Firebase never outlives the frontend's 25s login budget.
- **LocalStack stands in for S3 only** (photo storage); empty `cloud.aws.s3.endpoint` means real AWS.
- **Session model**: app-issued HS256 JWT; refresh tokens stored SHA-256-hashed in `sessoes_usuario`. `JwtAuthenticationFilter` re-validates against the DB on every request (user still active, role claim still equals current profile) — deactivation/role changes bite immediately, at the cost of one query per request. Authorization is deliberately double-layered: `SecurityConfig` matchers AND `@PreAuthorize` on controllers.

## Data-model invariants

- History is trigger-written, never by application code: `pessoa` → `pessoa_history` (AFTER INSERT/UPDATE), `person_note` → `person_note_history` (BEFORE UPDATE). Changing `pessoa` columns requires recreating `insert_pessoa_history()` in the same migration (precedent: V005).
- `updated_at` is set by a trigger — don't set it in code.
- Hard deletes only; there is no soft-delete. Delete history rows before the parent row (FK), as in `PessoaRepository.deletePregnancy`.
- `updated_by → usuario` is `ON DELETE RESTRICT` (V008): a user who authored any record cannot be deleted.
- Bilingual schema is intentional: legacy PT tables (`pessoa`, PK `pessoa_id`) coexist with EN tables (V006+), and EN tables FK into PT ones (`official_act.person_id → pessoa(pessoa_id)`). Do not "fix" the inconsistency.
- `CategoriaEnum` / `OfficialActFormEnum` / `OfficialActTypeEnum` hardcode DB seed ids — any seed change is a paired enum change. They serialize as objects and deserialize from a bare id (`@JsonFormat(OBJECT)` + delegating `fromId`).
- Name search is accent-insensitive (`unaccent` extension + `QueryConditions.addUnaccentedLike`).

## Domain invariants

- **Official acts**: `metadata` is JSONB validated in `OfficialActService` against the per-form schema encoded in `OfficialActFormEnum` (required fields; PERSON_REF shape). Creating an act applies side effects to the person — categoria, and possibly baptism/profession-of-faith/death dates — unless the form sets `skipEffects`. Deleting an act reverts **categoria only** (to the value held immediately before the act); dates stay.
- **Admission order numbers** come from the global sequence `official_act_admission_order_seq`; an MNC→MC promotion (Art. 24,d) reuses the person's latest existing number instead of drawing a new one. Suppressible per form.
- **Pregnancy**: a `pessoa` in categoria 29 (30 = temporary secrecy). Create copies the mother's campus and family-head address and creates MAE/PAI relationships; guards: father ≠ mother, due date not in the past. Registering a birth moves the baby to categoria 16 (aguardando batismo infantil) by default. `close` refuses while official acts reference the person, then hard-deletes (history first).

- **Serving areas** (V009, `serving_area` + `_position` / `_team` / `_member`): ministries/societies/governing bodies. Named `serving_area` (not `church_service`) to avoid colliding with the service layer; UI says "Serviço".
  - **Soft-delete exception (só o serviço)**: a área (`serving_area`) tem `active` — desativar oculta da listagem padrão; cargos e equipes não têm flag de ativo (exclusão só quando nada referencia). Membership history lives in `serving_area_member_history` (DB triggers on INSERT/UPDATE/DELETE; `deleted_at` set on removal).
  - **Vínculo atual** = row present in `serving_area_member` (ending a mandate = DELETE; history preserved). Drives supervisor uniqueness, listagem derivada (`teamCount`, `memberCount`, `coordinatorCount`), detail sections, the person card, and reports.
  - **Membresia com equipes**: quando o serviço tem ao menos uma equipe, vínculo `MEMBERSHIP` exige `teamId` (validado no service); coordenação pode ficar no escopo geral (`team_id` nulo) ou de equipe. **Supervisão é sempre do serviço inteiro**: um vínculo `SUPERVISION` com `teamId` é rejeitado.
  - **One supervisor per area** (a `SUPERVISION`-kind vínculo). Not auto-closed on change: the service rejects a second supervisor (409) — remove the current one first. The supervisor is conventionally a presbítero but this is **not** validated (no stable handle to "Conselho"; `system_key` was deliberately dropped).
  - **`position_id`/`team_id` FKs are `NO ACTION`** (not RESTRICT/SET NULL) so the area's `ON DELETE CASCADE` can drop members+cargos+equipes in one statement; "in use" protection lives in the service layer. `person_id → pessoa(pessoa_id) ON DELETE CASCADE` (person delete removes their vínculos).
  - **Cargo `kind` is immutable once the cargo has any vínculo** — else a MEMBERSHIP→SUPERVISION flip would retroactively promote everyone and bypass the supervisor rule. O nome do cargo segue editável. Listagem ordena por `kind` (SUPERVISION → COORDINATION → MEMBERSHIP), depois nome.
  - **Duplicate guard**: identical vínculos (same area+person+cargo+team) can't coexist (service-layer check; unique index `uq_serving_area_member` is the backstop).
  - **Seeds**: four fixed areas (Conselho, Junta Diaconal, Presbítero/Diácono em disponibilidade) created as ordinary areas, each with one MEMBERSHIP cargo. No special protection; operational rule is not to rename them.
  - **PG-native enum** `serving_area_position_kind` → plain string on the wire (`.name()` at the JOOQ boundary), not a seed-table enum.
  - **Report/member queries are generic filters** — `ParticipationReportQuery` and `ServingAreaMemberQuery` accept optional `categoryIds` + `campus` (equality) and the service passes them through as-is; the caller (FE) decides eligibility (serviços usam categorias 1-8 + campus SEDE). Não há regra de elegibilidade hardcoded no backend.
  - **Repositories** follow the `PessoaRepository.find(query)` shape: one generic `find(<Query>)` per repo (id is just a filter); writes return via `RETURNING` where there are no derived JOIN fields, otherwise the service re-fetches through `find`. Wire query objects are records (member query has a builder to avoid positional null-soup).

## Transactions / concurrency

- `@Transactional` per service write method (`readOnly = true` for reads); JOOQ rides the Spring-managed transaction. No optimistic locking — last write wins; low write contention is assumed (single congregation).
- Admission numbering relies on the PG sequence for concurrency safety.

## Out of scope / deprecated

- `informacoes_adicionais` was dropped in V005 (data migrated to `person_note`) — never reference it.
- Facebook login is a deliberate `UnsupportedOperationException` (501) stub.
- `scripts/backfill/` is a completed one-shot migration — don't rerun it.
- No multi-tenancy: single church by design.
