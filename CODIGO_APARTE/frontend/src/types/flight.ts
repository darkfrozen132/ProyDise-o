import type { Airport } from './airport';

export interface Flight {
  id: string;
  origin: Airport;
  destination: Airport;
  depLocal: string;
  arrLocal: string;
  dailyCapacity: number;
  cancelledDates: string[];
  originCode: string;
  destinationCode: string;
}