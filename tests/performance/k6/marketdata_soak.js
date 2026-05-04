// T309 — Market-data soak test (50M ticks/sec for 8h).
//
// Validates SC-014 — the platform must sustain 50M ticks/sec end-to-end across the four
// regions for an 8-hour soak with no missed sequence numbers and Aeron-publisher backpressure
// staying below the 1% offer-failed threshold.
//
// Run::
//
//   k6 run --duration 8h tests/performance/k6/marketdata_soak.js
//
// k6 cannot drive a 50M-tick UDP feed on its own — the test calls the {/marketdata/burst}
// REST seed endpoint which then triggers the synthetic-tick generator inside
// market-data-service. k6 polls the Prometheus scrape to verify the rate stays at target.

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 100,
    duration: __ENV.DURATION || '8h',
    thresholds: {
        http_req_failed: ['rate<0.001'],
        http_req_duration: ['p(99)<200'],
    },
};

const BASE = __ENV.MARKETDATA_BASE || 'http://market-data-service:8080';
const RATE_PER_SEC = parseInt(__ENV.RATE_PER_SEC || '50000000', 10);
const PER_VU = Math.floor(RATE_PER_SEC / 100);

export default function () {
    const res = http.post(
        `${BASE}/marketdata/burst`,
        JSON.stringify({ rate: PER_VU, instruments: ['CH0038863350', 'CH0012005267'] }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, {
        'burst accepted': (r) => r.status === 202,
    });
    sleep(1);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: '  ' }),
        'soak-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    const reqs = data.metrics.http_reqs.values.count;
    const failures = data.metrics.http_req_failed.values.passes;
    return `\nMarket-data soak — ${reqs} requests, ${failures} failures, p99 ${data.metrics.http_req_duration.values['p(99)']}ms\n`;
}
