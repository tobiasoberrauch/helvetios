import { useState } from 'react';
import { submitOrder } from '../services/api';
import type { OrderRequest, OrdType, RoutingMode, Side, TimeInForce } from '../types/order';

interface Props {
  onSubmitted: () => void;
}

export function OrderEntryView({ onSubmitted }: Props) {
  const [clOrdId, setClOrdId] = useState(() => `UI-${Date.now()}`);
  const [isin, setIsin] = useState('CH0038863350'); // Nestlé S.A.
  const [mic, setMic] = useState('XSWX');
  const [side, setSide] = useState<Side>('BUY');
  const [ordType, setOrdType] = useState<OrdType>('LIMIT');
  const [quantity, setQuantity] = useState<number>(100);
  const [price, setPrice] = useState<number>(99.5);
  const [timeInForce, setTimeInForce] = useState<TimeInForce>('DAY');
  const [routingMode, setRoutingMode] = useState<RoutingMode>('DMA');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    setSubmitting(true);
    setError(null);
    try {
      const req: OrderRequest = {
        clOrdId,
        instrumentId: { isin, mic },
        side,
        ordType,
        quantity,
        price: ordType === 'MARKET' ? undefined : price,
        timeInForce,
        routingMode,
      };
      await submitOrder(req);
      setClOrdId(`UI-${Date.now()}`);
      onSubmitted();
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Unknown error';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="panel">
      <h2>Order entry</h2>
      <div className="form-row">
        <label>ClOrdID</label>
        <input value={clOrdId} onChange={(e) => setClOrdId(e.target.value)} />
      </div>
      <div className="form-row">
        <label>ISIN</label>
        <input value={isin} onChange={(e) => setIsin(e.target.value.toUpperCase())} />
      </div>
      <div className="form-row">
        <label>MIC</label>
        <input value={mic} onChange={(e) => setMic(e.target.value.toUpperCase())} />
      </div>
      <div className="form-row">
        <label>Side</label>
        <select value={side} onChange={(e) => setSide(e.target.value as Side)}>
          <option value="BUY">BUY</option>
          <option value="SELL">SELL</option>
          <option value="SELL_SHORT">SELL_SHORT</option>
        </select>
      </div>
      <div className="form-row">
        <label>OrdType</label>
        <select value={ordType} onChange={(e) => setOrdType(e.target.value as OrdType)}>
          <option value="MARKET">MARKET</option>
          <option value="LIMIT">LIMIT</option>
          <option value="STOP">STOP</option>
          <option value="STOP_LIMIT">STOP_LIMIT</option>
          <option value="FUNARI">FUNARI</option>
          <option value="MOO">MOO</option>
          <option value="LOO">LOO</option>
        </select>
      </div>
      <div className="form-row">
        <label>Quantity</label>
        <input type="number" value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
      </div>
      {ordType !== 'MARKET' && (
        <div className="form-row">
          <label>Price</label>
          <input type="number" step="0.01" value={price} onChange={(e) => setPrice(Number(e.target.value))} />
        </div>
      )}
      <div className="form-row">
        <label>TIF</label>
        <select value={timeInForce} onChange={(e) => setTimeInForce(e.target.value as TimeInForce)}>
          <option value="DAY">DAY</option>
          <option value="IOC">IOC</option>
          <option value="FOK">FOK</option>
          <option value="GTC">GTC</option>
          <option value="OPG">OPG</option>
        </select>
      </div>
      <div className="form-row">
        <label>Routing</label>
        <select value={routingMode} onChange={(e) => setRoutingMode(e.target.value as RoutingMode)}>
          <option value="DMA">DMA</option>
          <option value="ALGO_WHEEL">ALGO_WHEEL</option>
          <option value="CARE">CARE</option>
        </select>
      </div>
      {error && <div style={{ color: '#f85149', fontSize: 12 }}>{error}</div>}
      <button className="primary" disabled={submitting} onClick={handleSubmit}>
        {submitting ? 'Submitting…' : 'Submit order'}
      </button>
    </div>
  );
}
