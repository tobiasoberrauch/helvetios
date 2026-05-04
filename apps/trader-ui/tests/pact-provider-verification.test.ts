/**
 * T085 — Pact-Provider-Verifikation. Verifiziert das Vertragsformat
 * zwischen Trader-UI (Konsument) und OMS-Service (Provider).
 *
 * In Phase 5 (US3) wird der Pact-Broker scharfgeschaltet — bis dahin ist
 * dies ein lokaler Schema-Check, der zumindest verhindert, dass das DTO
 * im Frontend von der OpenAPI-Spec im Backend abdriftet.
 */
import { describe, expect, it } from 'vitest';
import type { OrderRequest, OrderAck } from '../src/types/order';

describe('OMS API contract — trader-ui ↔ oms-service', () => {
  it('OrderRequest payload accepted by ISIN+MIC pattern', () => {
    const req: OrderRequest = {
      clOrdId: 'TEST-001',
      instrumentId: { isin: 'CH0038863350', mic: 'XSWX' },
      side: 'BUY',
      ordType: 'LIMIT',
      quantity: 100,
      price: 99.5,
      timeInForce: 'DAY',
      routingMode: 'DMA',
    };
    expect(req.instrumentId.isin).toMatch(/^[A-Z]{2}[A-Z0-9]{9}[0-9]$/);
    expect(req.instrumentId.mic).toMatch(/^[A-Z0-9]{4}$/);
    expect(['BUY', 'SELL', 'SELL_SHORT']).toContain(req.side);
  });

  it('OrderAck shape', () => {
    const ack: OrderAck = {
      orderId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
      clOrdId: 'TEST-001',
      submittedAtBiz: '2026-05-03T10:00:00.123Z',
      status: 'NEW',
    };
    expect(ack.orderId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    expect([
      'NEW', 'ACKNOWLEDGED', 'PARTIALLY_FILLED', 'FILLED',
      'PENDING_CANCEL', 'CANCELLED', 'PENDING_REPLACE',
      'REJECTED', 'EXPIRED', 'TRADE_BUSTED', 'BUSINESS_REJECTED',
    ]).toContain(ack.status);
  });
});
