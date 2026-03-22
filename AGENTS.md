# AGENTS.md

## Project Stage
- This project is in active development and requirements are still changing.
- Prefer small, reversible changes that improve structure without locking in premature abstractions.

## Package Structure
- `controller` contains HTTP endpoints only.
- `service` contains business and orchestration logic.
- `repository` contains Spring Data persistence access only.
- `model.dto` contains request and transfer models between layers.
- `model.entity` contains database entities mapped to PostgreSQL tables.
- `model.response` contains API response payloads and wrappers.
- `model.constant` contains shared constants or enums that are reused across the app.
- `config` contains Spring and infrastructure configuration.

## Coding Rules
- Do not return JPA entities directly from controllers.
- Use `BaseRestResponse`, `BaseSingleResponse`, and `BaseListResponse` for API responses.
- Business logic belongs in services, not controllers or repositories.
- Shared database columns belong in the base entity.
- Use meaningful names that reveal intent without extra explanation.
- Keep methods small and names explicit.
- Keep classes and methods focused on one responsibility.
- Prefer readable code over clever code.
- Prefer straightforward control flow over clever shortcuts.
- Remove duplication when it starts to hide intent or increase change risk.
- Avoid overengineering while requirements are still unstable.
- Add comments only when intent is not already obvious from the code.
- Leave touched code cleaner than you found it.

## Development Workflow
- Inspect existing code before making changes.
- Follow the controller -> service -> repository dependency direction.
- Keep DTOs, entities, and response models separate.
- Make the smallest change that moves the design forward.
- Refactor names and structure when confusion appears.
- Favor consistency across files over personal style preferences.
- Update documentation when the project structure or team conventions change.

## Testing Policy
- Do not write automated tests for now.
- Requirements and expected behavior are still unstable, so test coverage is intentionally deferred.
- When requirements become stable, introduce tests as a separate follow-up task.
