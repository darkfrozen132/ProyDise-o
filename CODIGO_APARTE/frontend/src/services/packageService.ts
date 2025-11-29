import { API } from "../api/api";

export interface Shipment {
  id?: number;
  code: string;
  origin: string;
  destination: string;
}

export const getPackages = async (): Promise<Shipment[]> => {
  const res = await API.get<Shipment[]>("/shipment");
  return res.data;
};

export const createPackage = async (pkg: Shipment): Promise<Shipment> => {
  const res = await API.post<Shipment>("/shipment", pkg);
  return res.data;
};
