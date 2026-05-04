// SC-013 — sustained 10M orders/day = ~115 orders/sec average. Peaks: 5x.
// k6 simulates 600 orders/sec for 5 minutes; OMS REST p99 should stay below 50ms.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 100 },   // ramp up
    { duration: '4m',  target: 600 },   // sustained 600 vu = ~600 req/s
    { duration: '30s', target: 0 },     // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(99)<50'],    // p99 < 50ms (per FR-020 cold-path budget)
    http_req_failed:   ['rate<0.001'],   // < 0.1% failure rate
  },
};

const OMS = __ENV.OMS_URL || 'http://localhost:8080';

export default function () {
  const payload = JSON.stringify({
    clOrdId: `K6-${randomString(16)}`,
    instrumentId: { isin: 'CH0038863350', mic: 'XSWX' },
    side: 'BUY',
    ordType: 'LIMIT',
    quantity: 100,
    price: 99.50,
    timeInForce: 'DAY',
    routingMode: 'DMA',
    preferredVenue: 'XSWX',
  });

  const res = http.post(`${OMS}/api/v1/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 202': (r) => r.status === 202,
    'has orderId': (r) => r.json('orderId') !== undefined,
  });

  sleep(0.1);
}
