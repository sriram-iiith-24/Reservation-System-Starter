package flight.reservation.order;

import flight.reservation.Customer;
import flight.reservation.flight.ScheduledFlight;

import java.util.List;

public class FlightCapacityHandler extends OrderValidationHandler {

    @Override
    public boolean handle(Customer customer, List<String> passengerNames, List<ScheduledFlight> flights) {
        boolean hasCapacity = flights.stream()
                .allMatch(sf -> sf.getAvailableCapacity() >= passengerNames.size());
        if (!hasCapacity) {
            throw new IllegalStateException("Not enough capacity on one or more flights");
        }
        return handleNext(customer, passengerNames, flights);
    }
}
