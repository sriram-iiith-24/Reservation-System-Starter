package flight.reservation.plane;

public class PassengerPlaneAdapter implements Aircraft {
    private final PassengerPlane plane;

    public PassengerPlaneAdapter(PassengerPlane plane) {
        this.plane = plane;
    }

    @Override
    public String getModel() {
        return plane.getModel();
    }

    @Override
    public int getPassengerCapacity() {
        return plane.getPassengerCapacity();
    }

    @Override
    public int getCrewCapacity() {
        return plane.getCrewCapacity();
    }
}
