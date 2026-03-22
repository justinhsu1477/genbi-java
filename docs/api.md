# API Documentation

## Health Endpoint

### Request

```http
GET /api/health
```

### Success Response

```json
{
  "success": true,
  "message": "Application is available",
  "data": {
    "applicationName": "gen-bi",
    "status": "UP",
    "totalModules": 1,
    "modules": [
      {
        "id": 1,
        "code": "CORE_HEALTH",
        "name": "Core Health Module",
        "description": "Starter module used to validate application structure."
      }
    ]
  }
}
```

### Response Notes

- The endpoint returns `BaseSingleResponse<HealthResponse>`.
- `modules` is sourced from the `system_modules` table.
- The seeded migration adds one starter module record for initial validation.
