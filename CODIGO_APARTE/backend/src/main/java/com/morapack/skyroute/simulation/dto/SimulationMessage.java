package com.morapack.skyroute.simulation.dto;

public record SimulationMessage(
        String simulationId,
        SimulationMessageType type,
        SimulationSnapshot snapshot,
        String error
) {

    public static SimulationMessage progress(String simulationId, SimulationSnapshot snapshot) {
        return new SimulationMessage(simulationId, SimulationMessageType.PROGRESS, snapshot, null);
    }

    public static SimulationMessage completed(String simulationId, SimulationSnapshot snapshot) {
        return new SimulationMessage(simulationId, SimulationMessageType.COMPLETED, snapshot, null);
    }

    public static SimulationMessage error(String simulationId, String error) {
        return new SimulationMessage(simulationId, SimulationMessageType.ERROR, null, error);
    }
}
