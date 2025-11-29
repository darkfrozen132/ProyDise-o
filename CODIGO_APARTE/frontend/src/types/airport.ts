export interface Airport {
  id: string;
  code: string;
  name: string;
  gmtOffsetHours: number;
  storageCapacity: number;
  continent: string;
  latitude: number;
  longitude: number;
}