"""
Tilt extension for apps/trader-ui/. Phase 3B.

Runs Vite dev-server outside of Kubernetes (faster inner loop) on
http://localhost:5173 with `/api` proxied to the OMS port-forward.
"""

local_resource(
    'trader-ui',
    serve_cmd='cd apps/trader-ui && npm install --no-fund --no-audit && npm run dev -- --host',
    deps=['apps/trader-ui/src', 'apps/trader-ui/package.json',
          'apps/trader-ui/index.html', 'apps/trader-ui/vite.config.ts'],
    readiness_probe=probe(
        period_secs=5,
        http_get=http_get_action(port=5173, path='/'),
    ),
    labels=['ui'],
)
