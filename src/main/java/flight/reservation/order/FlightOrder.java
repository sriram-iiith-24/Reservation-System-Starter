package flight.reservation.order;

import flight.reservation.flight.ScheduledFlight;
import flight.reservation.payment.PaymentStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlightOrder extends Order {
    private final List<ScheduledFlight> flights;
    static List<String> noFlyList = Arrays.asList("Peter", "Johannes");
    private final List<BookingObserver> observers = new ArrayList<>();

    public FlightOrder(List<ScheduledFlight> flights) {
        this.flights = flights;
    }

    public static List<String> getNoFlyList() {
        return noFlyList;
    }

    public List<ScheduledFlight> getScheduledFlights() {
        return flights;
    }

    public void addObserver(BookingObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (BookingObserver observer : observers) {
            observer.onBookingConfirmed(this);
        }
    }

    public boolean processOrder(PaymentStrategy paymentStrategy) throws IllegalStateException {
        if (isClosed()) {
            return true;
        }
        boolean isPaid = paymentStrategy.pay(this.getPrice());
        if (isPaid) {
            this.setClosed();
            notifyObservers();
        }
        return isPaid;
    }
}
