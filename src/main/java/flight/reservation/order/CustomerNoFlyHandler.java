package flight.reservation.order;

import flight.reservation.Customer;
import flight.reservation.flight.ScheduledFlight;

import java.util.List;

public class CustomerNoFlyHandler extends OrderValidationHandler {

    @Override
    public boolean handle(Customer customer, List<String> passengerNames, List<ScheduledFlight> flights) {
        if (FlightOrder.getNoFlyList().contains(customer.getName())) {
            throw new IllegalStateException("Customer is on the no-fly list");
        }
        return handleNext(customer, passengerNames, flights);
    }
}
