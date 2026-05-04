import { useEffect, useState } from 'react';
import { listOrders } from '../services/api';
import type { OrderDetail } from '../types/order';

interface Props {
  refreshKey: number;
}

export function OpenOrdersView({ refreshKey }: Props) {
  const [orders, setOrders] = useState<OrderDetail[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const data = await listOrders();
        if (!cancelled) setOrders(data);
      } finally {
        setLoading(false);
      }
    }
    void load();
    const interval = setInterval(load, 2000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [refreshKey]);

  return (
    <div className="panel">
      <h2>Orders {loading && '·'}</h2>
      <table>
        <thead>
          <tr>
            <th>ClOrdID</th>
            <th>Side</th>
            <th>Qty</th>
            <th>Px</th>
            <th>CumQty</th>
            <th>AvgPx</th>
            <th>Venue</th>
            <th>Status</th>
            <th>Last update</th>
          </tr>
        </thead>
        <tbody>
          {orders.length === 0 && (
            <tr>
              <td colSpan={9} style={{ textAlign: 'center', color: '#8b949e', padding: 24 }}>
                No orders yet — submit one on the left.
              </td>
            </tr>
          )}
          {orders.map((o) => (
            <tr key={o.orderId}>
              <td>{o.clOrdId}</td>
              <td>{o.side}</td>
              <td>{o.quantity}</td>
              <td>{o.price ?? 'MKT'}</td>
              <td>{o.cumQty}</td>
              <td>{o.avgPx ?? '—'}</td>
              <td>{o.executionVenue ?? '—'}</td>
              <td className={`status-${o.status}`}>{o.status}</td>
              <td>{new Date(o.lastUpdatedAt).toLocaleTimeString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
