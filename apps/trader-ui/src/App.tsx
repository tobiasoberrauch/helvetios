import { useEffect, useState } from 'react';
import { OrderEntryView } from './views/OrderEntryView';
import { OpenOrdersView } from './views/OpenOrdersView';
import { tryInitKeycloak } from './services/keycloak';

export function App() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);

  useEffect(() => {
    void tryInitKeycloak().then(setAuthenticated);
  }, []);

  return (
    <div className="app">
      <header className="topbar">
        <h1>Swiss TMS — Trader UI</h1>
        <div style={{ fontSize: 12, color: '#8b949e' }}>
          {authenticated === null ? 'auth: …' : authenticated ? 'auth: OIDC' : 'auth: disabled (dev)'}
        </div>
      </header>
      <main className="workspace">
        <OrderEntryView onSubmitted={() => setRefreshKey((k) => k + 1)} />
        <OpenOrdersView refreshKey={refreshKey} />
      </main>
    </div>
  );
}
