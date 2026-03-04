package flight.reservation.order;

import flight.reservation.Customer;
import flight.reservation.flight.ScheduledFlight;

import java.util.List;

public class PassengerNoFlyHandler extends OrderValidationHandler {

    @Override
    public boolean handle(Customer customer, List<String> passengerNames, List<ScheduledFlight> flights) {
        if (passengerNames.stream().anyMatch(p -> FlightOrder.getNoFlyList().contains(p))) {
            throw new IllegalStateException("A passenger is on the no-fly list");
        }
        return handleNext(customer, passengerNames, flights);
    }
}
