# OMS REST + gRPC API

**Service**: `apps/oms-service/`
**Authentication**: OAuth2 (Keycloak), JWT bearer tokens; required scopes per endpoint listed below.
**Rate limiting**: Spring Cloud Gateway with per-trader limits (default 50 req/sec).

REST endpoints are the trader-UI-facing surface; the high-throughput sell-side flow comes in via FIX (see `contracts/fix-sessions/inbound-acceptor-config.md`). gRPC endpoints are exposed for in-house automation tools.

## OpenAPI sketch (REST)

```yaml
openapi: 3.0.3
info:
  title: Swiss TMS — OMS API
  version: 1.0.0
servers:
  - url: https://oms.{region}.swisstms.local/api/v1
paths:
  /orders:
    post:
      summary: Submit a new order (trader UI / in-house client)
      security: [{ bearerAuth: [order:submit] }]
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/OrderRequest' }
      responses:
        '202': { description: Accepted; submitted to venue }
        '400': { description: Validation error }
        '403': { description: Entitlement denied }
        '422': { description: Pre-trade risk rejected }
    get:
      summary: List orders for the authenticated trader
      security: [{ bearerAuth: [order:read] }]
      parameters:
        - { name: status, in: query, schema: { type: string } }
        - { name: from,   in: query, schema: { type: string, format: date-time } }
        - { name: to,     in: query, schema: { type: string, format: date-time } }
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/Order' }
  /orders/{orderId}:
    get:
      summary: Get order detail (incl. executions and event history)
      security: [{ bearerAuth: [order:read] }]
      responses:
        '200':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/OrderDetail' }
  /orders/{orderId}/cancel:
    post:
      summary: Cancel an outstanding order
      security: [{ bearerAuth: [order:cancel] }]
      responses:
        '202': { description: Cancel request submitted }
        '409': { description: Order not in cancellable state }
  /orders/{orderId}/replace:
    post:
      summary: Replace (modify) an outstanding order
      security: [{ bearerAuth: [order:replace] }]
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/ReplaceRequest' }
      responses:
        '202': { description: Replace request submitted }
        '409': { description: Order not in replaceable state }
  /executions:
    get:
      summary: List executions for the trader (paginated)
      security: [{ bearerAuth: [execution:read] }]
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/Execution' }
  /algos/start:
    post:
      summary: Start an algo / SOR slice
      security: [{ bearerAuth: [algo:start] }]
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/AlgoStartRequest' }
      responses:
        '202':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ExecutionTask' }
  /audit/{orderId}:
    get:
      summary: Tamper-evident audit chain for an order (compliance-only)
      security: [{ bearerAuth: [audit:read] }]
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/AuditEvent' }

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
  schemas:
    OrderRequest:
      type: object
      required: [clOrdId, instrumentId, side, ordType, quantity, timeInForce]
      properties:
        clOrdId: { type: string, description: 'Client-supplied unique id' }
        instrumentId:
          type: object
          required: [isin, mic]
          properties:
            isin: { type: string, pattern: '^[A-Z]{2}[A-Z0-9]{9}[0-9]$' }
            mic:  { type: string, pattern: '^[A-Z0-9]{4}$' }
        side: { type: string, enum: [BUY, SELL, SELL_SHORT] }
        ordType: { type: string, enum: [MARKET, LIMIT, STOP, STOP_LIMIT, FUNARI, MOO, LOO] }
        quantity: { type: number, format: double, minimum: 0, exclusiveMinimum: true }
        price:    { type: number, format: double, minimum: 0, exclusiveMinimum: true }
        timeInForce: { type: string, enum: [DAY, IOC, FOK, GTC, GTD, OPG] }
        expireTime:  { type: string, format: date-time }
        routingMode: { type: string, enum: [DMA, ALGO_WHEEL, CARE] }
        algoStrategy:{ type: string, enum: [VWAP, TWAP, POV, IS] }
        algoParameters:
          type: object
          additionalProperties: { type: string }
        preferredVenue: { type: string, pattern: '^[A-Z0-9]{4}$' }
    Order:    # ... (read-side projection)
    OrderDetail: # ... (with executions[] and events[])
    Execution:   # ...
    ReplaceRequest: # ...
    AlgoStartRequest: # ...
    ExecutionTask: # ...
    AuditEvent: # ...
```

## gRPC sketch (proto)

```proto
syntax = "proto3";
package ch.swisstms.oms.v1;
option java_multiple_files = true;
option java_package = "ch.swisstms.oms.api.v1";

import "google/protobuf/timestamp.proto";

service OmsService {
  rpc SubmitOrder(SubmitOrderRequest) returns (OrderAck);
  rpc CancelOrder(CancelOrderRequest) returns (CancelAck);
  rpc ReplaceOrder(ReplaceOrderRequest) returns (ReplaceAck);
  rpc StreamExecutions(StreamExecutionsRequest) returns (stream Execution);
  rpc GetOrder(GetOrderRequest) returns (OrderDetail);
  rpc StartAlgo(StartAlgoRequest) returns (ExecutionTask);
}

message SubmitOrderRequest {
  string cl_ord_id = 1;
  string isin = 2;
  string mic = 3;
  Side side = 4;
  OrdType ord_type = 5;
  double quantity = 6;
  optional double price = 7;
  TimeInForce time_in_force = 8;
  optional google.protobuf.Timestamp expire_time = 9;
  RoutingMode routing_mode = 10;
  optional AlgoStrategy algo_strategy = 11;
  map<string, string> algo_parameters = 12;
  optional string preferred_venue = 13;
}

enum Side { SIDE_UNSPECIFIED = 0; BUY = 1; SELL = 2; SELL_SHORT = 3; }
enum OrdType { ORD_TYPE_UNSPECIFIED = 0; MARKET = 1; LIMIT = 2; STOP = 3; STOP_LIMIT = 4; FUNARI = 5; MOO = 6; LOO = 7; }
enum TimeInForce { TIF_UNSPECIFIED = 0; DAY = 1; IOC = 2; FOK = 3; GTC = 4; GTD = 5; OPG = 6; }
enum RoutingMode { ROUTING_MODE_UNSPECIFIED = 0; DMA = 1; ALGO_WHEEL = 2; CARE = 3; }
enum AlgoStrategy { ALGO_STRATEGY_UNSPECIFIED = 0; VWAP = 1; TWAP = 2; POV = 3; IS = 4; }

// ... (remaining messages elided for brevity; defined in contracts/proto/)
```

## Idempotency

`POST /orders` is idempotent on `clOrdId` per `clientId`. Repeated submissions of the same `clOrdId` return `200 OK` with the existing order's resource representation rather than creating a duplicate. `POST /orders/{orderId}/cancel` is idempotent (cancelling an already-cancelled order returns `200 OK` with no state change).

## Pact contracts

Generated Pact files for the trader-UI ↔ OMS contract live in `contracts/pact/trader-ui-oms.pact.json` and are verified in CI by `apps/trader-ui/` (consumer) and `apps/oms-service/` (provider).
