package flight.reservation.plane;

public class PassengerPlane {

    private final String model;
    private final int passengerCapacity;
    private final int crewCapacity;

    public PassengerPlane(String model) {
        int passengerCap;
        int crewCap;
        switch (model) {
            case "A380":
                passengerCap = 500;
                crewCap = 42;
                break;
            case "A350":
                passengerCap = 320;
                crewCap = 40;
                break;
            case "Embraer 190":
                passengerCap = 25;
                crewCap = 5;
                break;
            case "Antonov AN2":
                passengerCap = 15;
                crewCap = 3;
                break;
            default:
                throw new IllegalArgumentException(String.format("Model type '%s' is not recognized", model));
        }
        this.model = model;
        this.passengerCapacity = passengerCap;
        this.crewCapacity = crewCap;
    }

    public String getModel() {
        return model;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public int getCrewCapacity() {
        return crewCapacity;
    }

}
