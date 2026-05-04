import axios from 'axios';
import type { OrderAck, OrderDetail, OrderRequest } from '../types/order';

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

export async function submitOrder(req: OrderRequest): Promise<OrderAck> {
  const { data } = await client.post<OrderAck>('/orders', req);
  return data;
}

export async function listOrders(): Promise<OrderDetail[]> {
  const { data } = await client.get<OrderDetail[]>('/orders');
  return data;
}

export async function getOrder(orderId: string): Promise<OrderDetail> {
  const { data } = await client.get<OrderDetail>(`/orders/${orderId}`);
  return data;
}
