# Entitlements & Kill-Switch REST API

**Service**: `apps/entitlements-service/`
**Authentication**: OAuth2 (Keycloak); admin scopes only.

## OpenAPI sketch

```yaml
openapi: 3.0.3
info:
  title: Swiss TMS — Entitlements API
  version: 1.0.0
paths:
  /entitlements/{subjectType}/{subjectId}:
    get:
      summary: Read all entitlements for a subject
      security: [{ bearerAuth: [entitlement:read] }]
      responses: { '200': {} }
    put:
      summary: Replace the entitlement set for a subject
      security: [{ bearerAuth: [entitlement:write] }]
      responses: { '200': {} }
    patch:
      summary: Add / remove individual entitlements
      security: [{ bearerAuth: [entitlement:write] }]
      responses: { '200': {} }

  /killswitch/{scopeType}/{scopeId}/trip:
    post:
      summary: Trip the kill-switch for a scope
      security: [{ bearerAuth: [killswitch:trip] }]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [reason]
              properties:
                reason: { type: string, minLength: 10 }
      responses:
        '200': { description: Tripped }
        '409': { description: Already tripped }
  /killswitch/{scopeType}/{scopeId}/reset:
    post:
      summary: Reset the kill-switch (4-eyes — must be a different user from the tripper)
      security: [{ bearerAuth: [killswitch:reset] }]
      responses:
        '200': { description: Reset }
        '403': { description: Same-user-as-tripper rejected (4-eyes) }
        '409': { description: Not currently tripped }
  /killswitch/{scopeType}/{scopeId}:
    get:
      summary: Read kill-switch state
      security: [{ bearerAuth: [killswitch:read] }]
      responses: { '200': {} }
```

## Semantics

- All trip/reset operations write to the audit chain via `audit.command.v1`.
- The reset endpoint's 4-eyes check is enforced server-side by comparing the tripper's `UserId` (from the audit chain) with the resetter's `UserId` (from the JWT subject claim).
