package flight.reservation.plane;

public class PassengerDroneAdapter implements Aircraft {
    private final PassengerDrone drone;

    public PassengerDroneAdapter(PassengerDrone drone) {
        this.drone = drone;
    }

    @Override
    public String getModel() {
        return drone.getModel();
    }

    @Override
    public int getPassengerCapacity() {
        return drone.getPassengerCapacity();
    }

    @Override
    public int getCrewCapacity() {
        return drone.getCrewCapacity();
    }
}
