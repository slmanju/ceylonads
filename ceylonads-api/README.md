# CeylonAds — Phase 1 Backend

A package-by-domain Spring Boot monolith for a Sri Lankan classified ads marketplace.

## Stack

- Java 25 LTS
- Spring Boot 4.1.0
- Gradle
- Spring Security
- Username/password login with signed JWT
- Spring Data JPA
- H2 for local development
- PostgreSQL dependency/config ready for production
- Local filesystem media storage for local development
- OpenAPI + Swagger UI
- Dummy customers, admin, categories, locations, ads and sample SVG images

## Domains

- `auth` — account identity, registration, login, JWT, security
- `customer` — customer profile and customer operations
- `admin` — platform administration and ad moderation
- `ad` — ads and lifecycle
- `category` — hierarchical categories
- `location` — hierarchical Sri Lankan locations
- `media` — ad images and local storage
- `search` — search/filter specifications
- `common` — shared web/error/config concerns

## Run locally

Requires JDK 25 and Gradle 8.14+ or Gradle 9.x.

```bash
gradle bootRun --args='--spring.profiles.active=local'
```

The `local` profile is also the default profile, so this is enough:

```bash
gradle bootRun
```

### URLs

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console
- Sample media: http://localhost:8080/media/<filename>

H2 console:

- JDBC URL: `jdbc:h2:file:./data/ceylonads`
- User: `sa`
- Password: *(blank)*

## Seeded accounts

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `admin123` |
| CUSTOMER | `kamal` | `customer123` |
| CUSTOMER | `nimal` | `customer123` |

Change all seeded passwords before using this outside local development.

## Example login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"kamal","password":"customer123"}'
```

Copy the returned `accessToken`, then:

```bash
TOKEN="<token>"

curl http://localhost:8080/api/customers/me \
  -H "Authorization: Bearer $TOKEN"
```

## Browse/search ads

```bash
curl "http://localhost:8080/api/ads"
curl "http://localhost:8080/api/ads?q=toyota"
curl "http://localhost:8080/api/ads?category=vehicles&location=colombo"
curl "http://localhost:8080/api/ads?minPrice=100000&maxPrice=10000000"
```

## Create an ad

```bash
curl -X POST http://localhost:8080/api/ads \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"Samsung Galaxy S25",
    "description":"Excellent condition, complete box.",
    "price":245000,
    "categorySlug":"mobile-phones",
    "locationSlug":"colombo"
  }'
```

New customer ads start as `PENDING_REVIEW`.

## Upload media

```bash
curl -X POST http://localhost:8080/api/ads/1/media \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@photo.jpg"
```

## Admin moderation

Login as `admin`, then:

```bash
ADMIN_TOKEN="<admin-token>"

curl http://localhost:8080/api/admin/ads/pending \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X PATCH http://localhost:8080/api/admin/ads/1/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Production database profile

The included `prod` profile is prepared for PostgreSQL:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=jdbc:postgresql://host:5432/db \
DATABASE_USERNAME=user \
DATABASE_PASSWORD=secret \
JWT_SECRET_BASE64=<base64-encoded-32+-byte-secret> \
java -jar build/libs/ceylonads-0.0.1-SNAPSHOT.jar
```

`prod` intentionally does **not** pretend local filesystem media is durable on Cloud Run. Add the Supabase/Object Storage `MediaStorage` implementation before production deployment.

## Architecture notes

- Authentication identities live in `auth.Account`; business roles are `CUSTOMER` and `ADMIN`.
- Customer-specific profile data lives in the `customer` domain.
- Admin is an operational domain, not a duplicated authentication system.
- Public listing endpoints return only `ACTIVE` ads.
- Customers can mutate only their own ads.
- Admin endpoints are protected with `ROLE_ADMIN`.
- Local media is behind the `MediaStorage` interface so production object storage can replace it cleanly.

## Token-efficient code exploration

Minimize repository exploration before making changes.

- Do NOT inspect the entire project structure by default.
- Do NOT read unrelated controllers, services, repositories, entities, or configuration.
- Start with targeted search (`rg`, grep, filename search) based on the requested feature.
- Read only files directly relevant to the task.
- Follow dependencies only when required to understand or modify behavior.
- Do not reread files already inspected unless necessary.
- Do not inspect build/configuration files unless the task requires them.
- Prefer targeted edits over broad architectural analysis.

For small or localized changes:
1. Search for the relevant class/method/endpoint.
2. Inspect the minimum necessary files.
3. Make the change.
4. Run focused tests/verification.

Only perform broad repository exploration when the task explicitly involves architecture,
cross-cutting changes, or unfamiliar project structure.