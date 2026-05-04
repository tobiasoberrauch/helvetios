// T310 — Concurrent-trader test (10k concurrent sessions).
//
// Validates SC-015. Each VU simulates one trader hammering the OMS REST API with order
// submission + cancel cycles. 10k VUs are too heavy for a single k6 host; CI splits the test
// across 10 parallel runners with 1k VUs each via the k6-operator.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const submitLatency = new Trend('order_submit_latency', true);

export const options = {
    scenarios: {
        traders: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m',  target: 1000 },
                { duration: '2m',  target: 5000 },
                { duration: '5m',  target: parseInt(__ENV.MAX_VUS || '10000', 10) },
                { duration: '15m', target: parseInt(__ENV.MAX_VUS || '10000', 10) },
                { duration: '1m',  target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        order_submit_latency: ['p(99)<150'],
        http_req_failed: ['rate<0.001'],
    },
};

const BASE = __ENV.OMS_BASE || 'http://oms-service:8080';

export default function () {
    const traderId = `t-${__VU}`;
    const submitRes = http.post(
        `${BASE}/api/v1/orders`,
        JSON.stringify({
            clientId: traderId,
            isin: 'CH0038863350',
            mic: 'XSWX',
            side: 'BUY',
            qty: 100,
            limitPrice: 105.40,
            timeInForce: 'DAY',
            routingMode: 'DMA',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    submitLatency.add(submitRes.timings.duration);
    check(submitRes, {
        'order accepted': (r) => r.status === 202,
    });
    if (submitRes.status === 202) {
        const orderId = submitRes.json('orderId');
        sleep(0.05);
        http.del(`${BASE}/api/v1/orders/${orderId}`);
    }
    sleep(0.1);
}
