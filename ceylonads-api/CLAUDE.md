# CeylonAds Backend Instructions

These rules apply to work inside `ceylonads-api/`.

## Stack

Use the versions already declared by the project.

Core stack:

```text
Java
Spring Boot
Gradle
Spring Web
Spring Data JPA
Spring Security
JJWT
Swagger/OpenAPI
H2 locally
PostgreSQL in production
```

Do not change Java, Spring Boot, Gradle, or dependency versions unless explicitly requested.

## Working style

* Make focused changes only for the current task.
* Do not perform unrelated refactors.
* Preserve existing working behavior unless the task explicitly changes it.
* Reuse existing services, DTOs, repositories, utilities, security components, and patterns before creating new ones.
* Prefer simple, standard Spring solutions over unnecessary abstractions.
* Keep changes small enough to review.
* Do not redesign neighboring domains unless the current task requires it.

## Context discipline

Keep repository exploration narrow.

* Start with the affected business domain.
* Inspect only directly related shared/security/configuration code when required.
* Do not recursively scan every backend domain for normal feature work.
* Prefer targeted searches for class names, endpoints, methods, fields, and configuration.
* Do not repeatedly reread large unchanged files.
* Do not inspect `build/`, Gradle caches, generated files, IDE files, or other irrelevant output.
* Do not dump large seed-data files, generated SQL, full build logs, or unrelated test output into context unless required.
* When command output is large, focus on the relevant failure/error section.
* Do not perform repo-wide architecture analysis unless explicitly requested.

## Agent / command discipline

For ordinary backend feature work:

* Do not spawn subagents.
* Do not use `/run` unless explicitly requested or clearly necessary.
* Prefer direct targeted file inspection and commands.
* Do not create parallel investigations for work that can be completed directly.
* Use subagents only when the task genuinely benefits from independent or parallel investigation.
* Avoid broad automated review passes after a focused implementation unless explicitly requested.

## Package structure

Keep package-by-domain:

```text
com.slmanju.ceylonads
├── auth
├── customer
├── admin
├── ad
├── category
├── location
├── media
├── search
├── promotion
├── payment
└── common
```

Inside a domain, use standard packages only where needed:

```text
controller
service
repository
entity
dto
mapper
security
specification
storage
```

Do not reorganize the backend into global layer packages.

Do not create empty packages merely to satisfy a diagram or desired structure.

## Spring practices

* Controllers are thin.
* Controllers do not call repositories directly.
* Business rules belong in services.
* Persistence belongs in repositories.
* Use constructor injection; do not use field injection.
* Use DTOs for REST contracts; do not expose JPA entities directly.
* Use Bean Validation and `@Valid`.
* Use `@Transactional` at meaningful service boundaries.
* Prefer `@Transactional(readOnly = true)` for substantial read flows where appropriate.
* Prefer LAZY relationships unless there is a strong reason otherwise.
* Avoid `@Data` on JPA entities.
* Avoid unnecessary bidirectional mappings.
* Avoid N+1 query behavior.
* Use `BigDecimal` for money.
* Use enums for controlled lifecycle/status values.
* Keep mapping explicit and understandable.
* Do not introduce MapStruct unless explicitly justified.
* Do not introduce Spring Modulith.

## API rules

* Preserve endpoint paths and response contracts unless the task intentionally changes them.
* Update Swagger/OpenAPI when an API contract changes.
* Keep request/response DTOs explicit.
* Public list/search endpoints must remain pageable.
* Do not expose arbitrary entity-property sorting directly to clients.
* Validate ownership and authorization in backend services.
* `CUSTOMER` must never mutate another customer's data by guessing IDs.
* `ADMIN` privileges must be enforced server-side.
* Do not rely on frontend authorization for security.

## Authentication and security

Authentication is intentionally simple:

```text
username/password
Spring Security
BCrypt
JJWT
custom JWT filter
```

Roles:

```text
CUSTOMER
ADMIN
```

Do not introduce:

```text
OAuth2 Authorization Server
OAuth2 Resource Server
Cognito
Keycloak
social login
```

unless explicitly requested.

Do not weaken existing authorization rules while implementing unrelated features.

## Errors

Use centralized exception handling.

Return appropriate HTTP status codes and useful client-safe error responses.

Do not expose:

* stack traces
* SQL details
* password hashes
* JWT internals
* internal exception details
* sensitive configuration values

## Database

Local development uses H2.

Production direction is PostgreSQL.

Do not introduce H2-specific application logic that would break PostgreSQL compatibility.

For search/filter queries:

* filter in the database
* preserve pagination
* avoid loading all rows and filtering in Java
* avoid unnecessary query proliferation
* consider indexes for production-relevant searchable/filterable fields
* use explicit repository queries/specifications when they improve clarity

Do not couple application behavior to generated or seeded database IDs.

## Media

Keep storage behind the existing abstraction.

Local development:

```text
filesystem
```

Production direction:

```text
object storage
```

Do not persist image bytes in the relational database unless explicitly requested.

Do not introduce cloud-storage dependencies for normal local development.

## Seed data

Seed data is for local/demo use only.

Keep examples realistic for areas such as:

```text
vehicles
property
mobiles/electronics
tuition
services
customers
admins
promotions
```

When modifying seed data:

* do not couple application logic to seeded IDs
* do not make seed generation part of production behavior
* avoid unnecessarily large hard-coded data structures
* prefer small reusable builders/generators when many examples are needed
* do not load or rewrite unrelated seed datasets for a focused change

## Testing and verification

During implementation:

* Run targeted tests for the affected domain when useful.
* Prefer a focused test class or package over the entire test suite.
* Do not repeatedly run the full backend test suite after every small edit.
* When a test fails, inspect the relevant failure rather than dumping/re-reading all output.

Before completion:

```bash
./gradlew build
```

Run the final build once when appropriate.

`build` already executes the normal test lifecycle, so do not also run a separate full `./gradlew test` immediately beforehand unless there is a specific reason.

If the task changes only documentation or otherwise does not require a build, use judgment rather than performing unnecessary verification.

Do not finish with known compile or test failures caused by the change.

## Scope control

When a task affects one domain, remain within that domain plus directly required dependencies.

Examples:

```text
promotion task
→ promotion
→ payment only if directly required
→ common/security only if directly required
```

Do not automatically inspect or modify:

```text
auth
ad
search
media
customer
admin
```

unless the requested behavior depends on them.

Cross-domain changes are acceptable when genuinely required, but keep them explicit and minimal.

## Before editing unrelated code

If implementation appears to require a significant unrelated refactor:

1. verify that it is genuinely required
2. prefer a smaller compatible change if possible
3. do not expand the task merely to clean up existing code
4. mention a genuine follow-up concern in the final report instead of fixing it opportunistically

## Final response

Keep completion reports concise.

Report only:

* what changed
* important API/schema/database changes
* build/test result
* genuine remaining gaps or follow-up concerns

Do not repeat the full implementation plan.
Do not provide a long file-by-file narration unless explicitly requested.
