package flight.reservation.order;

import flight.reservation.flight.ScheduledFlight;
import flight.reservation.payment.PaymentStrategy;

import java.util.Arrays;
import java.util.List;

public class FlightOrder extends Order {
    private final List<ScheduledFlight> flights;
    static List<String> noFlyList = Arrays.asList("Peter", "Johannes");

    public FlightOrder(List<ScheduledFlight> flights) {
        this.flights = flights;
    }

    public static List<String> getNoFlyList() {
        return noFlyList;
    }

    public List<ScheduledFlight> getScheduledFlights() {
        return flights;
    }

    public boolean processOrder(PaymentStrategy paymentStrategy) throws IllegalStateException {
        if (isClosed()) {
            return true;
        }
        boolean isPaid = paymentStrategy.pay(this.getPrice());
        if (isPaid) {
            this.setClosed();
        }
        return isPaid;
    }
}
