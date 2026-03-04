package flight.reservation.plane;

public class AircraftFactory {
    public static Aircraft create(String model) {
        switch (model) {
            case "A380":
            case "A350":
            case "Embraer 190":
            case "Antonov AN2":
                return new PassengerPlaneAdapter(new PassengerPlane(model));
            case "H1":
            case "H2":
                return new HelicopterAdapter(new Helicopter(model));
            case "HypaHype":
                return new PassengerDroneAdapter(new PassengerDrone(model));
            default:
                throw new IllegalArgumentException(
                        String.format("Aircraft model '%s' is not recognized", model));
        }
    }
}
