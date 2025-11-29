import type { Airport } from './airport';

export interface Order {
  id: string;
  customerReference: string;
  destinationAirport: Airport;
  quantity: number;
  creationUtc: string;
  dueUtc: string;
  scope: 'REAL' | 'PROJECTED';
}

export interface OrderPage {
  items: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
