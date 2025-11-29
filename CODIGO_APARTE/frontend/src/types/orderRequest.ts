export interface OrderRequest {
  id: string;
  customerReference: string;
  destinationAirportCode: string;
  quantity: number;
  creationLocal: string;
  projected?: boolean;
}
