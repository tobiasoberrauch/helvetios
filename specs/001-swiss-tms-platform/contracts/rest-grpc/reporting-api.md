# Reporting Service Control API

**Service**: `apps/reporting-service/`
**Authentication**: OAuth2 (Keycloak); compliance and ops scopes.

The reporting service is primarily driven by Spring Batch on a schedule; this REST API is the human / ops control surface.

## OpenAPI sketch

```yaml
openapi: 3.0.3
info:
  title: Swiss TMS — Reporting API
  version: 1.0.0
paths:
  /reports/{reportType}/run:
    post:
      summary: Trigger a report job manually (compliance-controlled)
      security: [{ bearerAuth: [reporting:trigger] }]
      parameters:
        - { name: reportType, in: path, required: true, schema: { type: string, enum: [FINFRAG_ART39, RTS22, TRAX_APA, EMIR_DTCC, EMIR_REGIS] } }
        - { name: reportingDate, in: query, required: true, schema: { type: string, format: date } }
        - { name: dryRun, in: query, schema: { type: boolean, default: false } }
      responses:
        '202':
          description: Job started
          content:
            application/json:
              schema:
                type: object
                properties:
                  reportId: { type: string }
                  status: { type: string }
  /reports/{reportId}:
    get:
      summary: Get report status, payload reference, and submission acknowledgment
      security: [{ bearerAuth: [reporting:read] }]
      responses:
        '200':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/TransactionReport' }
  /reports/{reportType}:
    get:
      summary: List recent reports
      security: [{ bearerAuth: [reporting:read] }]
      parameters:
        - { name: from, in: query, schema: { type: string, format: date } }
        - { name: to,   in: query, schema: { type: string, format: date } }
        - { name: status, in: query, schema: { type: string } }
      responses: { '200': {} }
  /reports/{reportId}/payload:
    get:
      summary: Download the canonical XML payload (compliance-only; logged to audit)
      security: [{ bearerAuth: [reporting:download] }]
      responses:
        '200':
          content:
            application/xml: {}

  /audit/{orderId}:
    get:
      summary: Auditor view — full reporting trail for a given order
      security: [{ bearerAuth: [audit:read] }]
      responses: { '200': {} }
```

## Semantics

- Manual triggers are gated on `dryRun=true` by default in non-production environments to prevent accidental regulator submissions.
- Every report download is logged to `audit.command.v1` with the user identity and the `reportId`.
- Payload retrieval returns the **canonical** XML (the bytes whose SHA-256 is the `payloadXmlSha256` of the `TransactionReport`) so auditors can verify the regulator submission against the platform's audit trail.
