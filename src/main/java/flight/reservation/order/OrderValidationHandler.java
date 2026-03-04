package flight.reservation.order;

import flight.reservation.Customer;
import flight.reservation.flight.ScheduledFlight;

import java.util.List;

public abstract class OrderValidationHandler {
    private OrderValidationHandler next;

    public OrderValidationHandler setNext(OrderValidationHandler next) {
        this.next = next;
        return next;
    }

    public abstract boolean handle(Customer customer, List<String> passengerNames, List<ScheduledFlight> flights);

    protected boolean handleNext(Customer customer, List<String> passengerNames, List<ScheduledFlight> flights) {
        if (next == null) return true;
        return next.handle(customer, passengerNames, flights);
    }
}
