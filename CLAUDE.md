# ipredencao-manager-api

Java 21 · Spring Boot 3.5 · JOOQ 3.19 DB-first codegen · Liquibase · Postgres 17 · Gradle. No JPA, no Lombok.

## Prerequisites

Java 21, Docker + Compose v2. Docker Engine 29+ only works because `support/IntegrationTestBase` forces `System.setProperty("api.version", "1.44")` in a static block — never remove it.

## Commands (verified)

| Command | What it does |
|---|---|
| `docker compose up -d` | Postgres on host port **54329** (not 5432) + LocalStack S3 on 4566 |
| `make run` | compose up + migrations + JOOQ codegen + `bootRun` (health: `/actuator/health`) |
| `./gradlew update -x composeUp` | apply Liquibase migrations only |
| `make jooq-only` (= `./gradlew generateJooq -x composeUp`) | regenerate JOOQ; DB must be up + migrated |
| `./gradlew compileJava -x generateJooq` | compile without codegen |
| `./gradlew test` | full integration suite (Testcontainers; failFast, -Xmx512m) |
| `./gradlew test --tests '*.repository.PessoaRepositoryIT'` | one test class |
| `make restart` / `make clean` | recreate DB (clean also prunes volumes) |

## Hard rules

- Never introduce JPA/Hibernate/Spring Data or Lombok: repositories are hand-written JOOQ DSL; models have manual getters/setters.
- Models use Joda-Time; `java.time` exists only at the JOOQ boundary — convert exclusively via `util/DateTimeHelper`.
- Never edit an applied migration. New ones: `db/changelog/V###__topic.sql` (list the folder for the next number) + `<include>` in `db.changelog-master.xml`, or Liquibase ignores it.
- Match seed enums by id (`fromId`), never by name — V001 seeds contain a typo (categoria 6 `'Eespecial…'`) that `CategoriaEnum` spells correctly.
- `apprunner-config.json` and the Firebase admin-SDK JSON hold real secrets — never echo or commit new ones; use the `.example` siblings.
- Comments/Javadoc in production code: Portuguese (church domain vocabulary). Tests may use English names/comments.
- Ship doc updates with the feature: if it adds/changes an architecture decision, data-model or domain invariant, or deprecates something, update `SPEC.md` (this folder) in the same PR; if commands, prerequisites or gotchas change, update this file.

## Gotchas

- Generated JOOQ classes live in `target/generated-sources/jooq` (gitignored): a fresh clone doesn't compile until the DB is up and migrated. Regenerate after every schema change.
- Local app boot does NOT run Liquibase (`spring.liquibase.enabled=false`); migrations come from `./gradlew update`. Prod and test profiles run it at boot.
- The Gradle Liquibase config sets `searchPath 'src/main/resources'` so recorded changelog paths match Spring Boot's classpath paths — don't "simplify" it or checksums diverge.
- `composeUp` hardcodes `/usr/local/bin/docker`; `update` skips compose when `CI` is set.
- Adding a value to a Postgres-native enum (`sexo`, `perfil_acesso`, …) requires an `ALTER TYPE` migration; JOOQ mirrors them as Java enums converted by `.name()`.
- Tests are integration-only (no mock-based unit tests — the domain is coupled to JOOQ/Liquibase/triggers). Base: `support/IntegrationTestBase`; fixtures: `support/*Fixture`; controller auth via `@WithMockUser(roles = "PRESBITERO"|"ADMIN"|…)`.
- `src/main/resources/IPR_Dump/` ships thousands of seed photos into the jar — don't glob or relocate it casually.
- Windows: use `run.bat` / `make win-*` targets.

## Pointers

- `docs/ERROR_HANDLING.md` — exception→HTTP mapping and handler layout.
- `docs/` — S3 setup, CORS, auth integration, intake-form endpoint runbooks (Portuguese).
- `ENV_VARS_CHECKLIST.md` — prod (App Runner/RDS) env-var checklist.
- `scripts/backfill/README.md` — the completed person_note→official_act migration.
- `lambda/firebase-proxy/` — separate Maven project with its own `deploy.sh`; not part of the Gradle build.
