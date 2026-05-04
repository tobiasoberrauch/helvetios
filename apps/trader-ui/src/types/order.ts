export type Side = 'BUY' | 'SELL' | 'SELL_SHORT';
export type OrdType = 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT' | 'FUNARI' | 'MOO' | 'LOO';
export type TimeInForce = 'DAY' | 'IOC' | 'FOK' | 'GTC' | 'GTD' | 'OPG';
export type RoutingMode = 'DMA' | 'ALGO_WHEEL' | 'CARE';
export type OrdStatus =
  | 'NEW'
  | 'ACKNOWLEDGED'
  | 'PARTIALLY_FILLED'
  | 'FILLED'
  | 'PENDING_CANCEL'
  | 'CANCELLED'
  | 'PENDING_REPLACE'
  | 'REJECTED'
  | 'EXPIRED'
  | 'TRADE_BUSTED'
  | 'BUSINESS_REJECTED';

export interface InstrumentDto {
  isin: string;
  mic: string;
}

export interface OrderRequest {
  clOrdId: string;
  instrumentId: InstrumentDto;
  side: Side;
  ordType: OrdType;
  quantity: number;
  price?: number;
  timeInForce: TimeInForce;
  routingMode: RoutingMode;
  preferredVenue?: string;
}

export interface OrderAck {
  orderId: string;
  clOrdId: string;
  submittedAtBiz: string;
  status: OrdStatus;
}

export interface OrderDetail {
  orderId: string;
  clOrdId: string;
  status: OrdStatus;
  side: Side;
  ordType: OrdType;
  quantity: number;
  price: number | null;
  cumQty: number;
  leavesQty: number;
  avgPx: number | null;
  executionVenue: string | null;
  submittedAtBiz: string;
  lastUpdatedAt: string;
}
