# CeylonAds — Claude Code Instructions

## Project

CeylonAds is a Sri Lankan classified-ads marketplace.

Repository shape:

```text
ceylonads/
├── ceylonads-api/   Spring Boot backend
├── ceylonads-ui/        React + Vite + TypeScript
└── CLAUDE.md
```

## Working style

- Make focused changes only for the current task.
- Do not perform unrelated refactors.
- Preserve existing working behavior unless the task explicitly changes it.
- Prefer simple, standard, maintainable solutions over clever abstractions.
- Reuse existing components, services, DTOs, utilities, and patterns before creating new ones.
- Do not introduce a new framework/library unless it provides clear value.
- Keep changes small enough to review.

## Context / token discipline

- Do not recursively scan the entire repository.
- Start with files directly relevant to the requested feature.
- For frontend API work, use Swagger/OpenAPI as the primary backend contract.
- Do not read Java controllers/entities/DTOs just to discover contracts already available in OpenAPI.
- Inspect a backend source file only when Swagger is genuinely insufficient for a specific question.
- Do not repeatedly reread large unchanged files.
- Do not produce long planning documents before implementation unless explicitly requested.
- At completion, provide a concise report.

Backend OpenAPI during local development:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui.html
```

## Architecture

Use a monolith organized by business domain.

Backend top-level domains include:

```text
auth
customer
admin
ad
category
location
media
search
promotion
payment
common
```

Do not introduce Spring Modulith.

Backend domains use normal internal Spring packages where useful:

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

ceylonads-ui Frontend is React + Vite + TypeScript and remains feature-oriented.

## Roles and authentication

Initial roles:

```text
CUSTOMER
ADMIN
```

Authentication is intentionally simple:

```text
username/password
Spring Security
BCrypt
JJWT
custom JWT filter
```

Do not introduce OAuth2 Authorization Server, OAuth2 Resource Server, Cognito, Keycloak, or social login unless explicitly requested.

## Local development

Backend local development:

```text
H2
local filesystem media
```

Production direction:

```text
PostgreSQL
object storage
containerized Spring Boot
```

Do not require cloud services for normal local development.

## UI identity

CeylonAds is a classifieds marketplace, not an ecommerce shopping-cart application.

Public UI direction:

```text
teal + slate + warm neutral
clean
compact
trustworthy
marketplace-oriented
```

Avoid a blue-heavy ecommerce look.

Do not add heart/favorite controls unless favorites are explicitly implemented.

## Definition of done

Backend changes normally run:

```bash
./gradlew test
./gradlew build
```

Frontend changes normally run:

```bash
npm run build
```

Do not finish with known compile errors.

## Final response

Keep completion reports concise:

- what changed
- important API/schema changes
- tests/build result
- genuine remaining gaps

Do not repeat the full implementation plan.
