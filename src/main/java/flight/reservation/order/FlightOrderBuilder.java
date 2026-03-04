package flight.reservation.order;

import flight.reservation.Customer;
import flight.reservation.Passenger;
import flight.reservation.flight.ScheduledFlight;

import java.util.List;

public class FlightOrderBuilder {

    private List<ScheduledFlight> flights;
    private Customer customer;
    private double price;
    private boolean priceSet = false;
    private List<Passenger> passengers;

    public FlightOrderBuilder withFlights(List<ScheduledFlight> flights) {
        this.flights = flights;
        return this;
    }

    public FlightOrderBuilder withCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public FlightOrderBuilder withPrice(double price) {
        this.price = price;
        this.priceSet = true;
        return this;
    }

    public FlightOrderBuilder withPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
        return this;
    }

    public FlightOrder build() {
        if (flights == null || flights.isEmpty()) {
            throw new IllegalStateException("FlightOrder requires at least one flight.");
        }
        if (customer == null) {
            throw new IllegalStateException("FlightOrder requires a customer.");
        }
        if (!priceSet) {
            throw new IllegalStateException("FlightOrder requires a price.");
        }
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalStateException("FlightOrder requires at least one passenger.");
        }
        FlightOrder order = new FlightOrder(flights);
        order.setCustomer(customer);
        order.setPrice(price);
        order.setPassengers(passengers);
        return order;
    }
}
