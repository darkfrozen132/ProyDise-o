// Corresponde a SimulationSegment.java
export interface SimulationSegment {
  flightId: string;
  origin: string;
  destination: string;
  date: string;
  quantity: number;
  departureUtc: string;
  arrivalUtc: string;
}

// Corresponde a SimulationRoute.java
export interface SimulationRoute {
  quantity: number;
  slackMinutes: number;
  segments: SimulationSegment[];
}

// Corresponde a SimulationOrderPlan.java
export interface SimulationOrderPlan {
  orderId: string;
  slackMinutes: number;
  routes: SimulationRoute[];
}

// Corresponde a SimulationSnapshot.java
export interface SimulationSnapshot {
  simulationId: string;
  processedOrders: number;
  totalOrders: number;
  fitness: number;
  generatedAt: string;
  orderPlans: SimulationOrderPlan[];
}

// Corresponde a SimulationMessageType.java
export type SimulationMessageType = "PROGRESS" | "COMPLETED" | "ERROR";

// Corresponde a SimulationMessage.java
export interface SimulationMessage {
  simulationId: string;
  type: SimulationMessageType;
  snapshot: SimulationSnapshot | null;
  error: string | null;
}

// Corresponde a SimulationStartRequest.java
export interface SimulationStartRequest {
  startDate?: string; // ISO 8601 sin zona
  endDate?: string;
  windowMinutes?: number;
}

// Corresponde a SimulationStartResponse.java
export interface SimulationStartResponse {
  simulationId: string;
}

// Corresponde a SimulationStatus.java
export interface SimulationStatus {
  simulationId: string;
  processedOrders: number;
  totalOrders: number;
  completed: boolean;
  cancelled: boolean;
  error: string | null;
  lastSnapshot: SimulationSnapshot | null;
}
