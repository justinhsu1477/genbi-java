# gen-bi

Spring Boot foundation for BI generation services with a package-based structure, PostgreSQL persistence, and Flyway migrations.

## Current status

This repository is still in the development stage. Requirements are not stable yet, so the focus is on clean structure and maintainable code instead of automated test coverage for now.

## Package structure

- `controller`: REST endpoints
- `service`: business and orchestration logic
- `repository`: Spring Data JPA repositories
- `model.dto`: request and transfer models
- `model.entity`: PostgreSQL entities
- `model.response`: standardized API responses
- `model.constant`: shared constants
- `config`: Spring configuration

## Database setup

The application uses PostgreSQL and Flyway.

Required environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/gen_bi
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

Flyway migrations are located in `src/main/resources/db/migration`.

## Run the application

```bash
mvn spring-boot:run
```

Sample endpoint:

```text
GET /api/health
```

## Development notes

- Controllers should never return entities directly.
- Use `BaseRestResponse`, `BaseSingleResponse`, or `BaseListResponse` for API payloads.
- Business logic belongs in services.
- Shared database fields should live in the base entity.
- Do not add automated tests yet; revisit testing after requirements stabilize.

## Documentation

- Contributor guide: `AGENTS.md`
- API docs: `docs/api.md`
