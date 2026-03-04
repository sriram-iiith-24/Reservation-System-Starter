package flight.reservation;

import flight.reservation.flight.ScheduledFlight;
import flight.reservation.order.CustomerNoFlyHandler;
import flight.reservation.order.FlightCapacityHandler;
import flight.reservation.order.FlightOrder;
import flight.reservation.order.Order;
import flight.reservation.order.OrderValidationHandler;
import flight.reservation.order.PassengerNoFlyHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Customer {

    private String email;
    private String name;
    private List<Order> orders;

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
        this.orders = new ArrayList<>();
    }

    public FlightOrder createOrder(List<String> passengerNames, List<ScheduledFlight> flights, double price) {
        OrderValidationHandler chain = new CustomerNoFlyHandler();
        chain.setNext(new PassengerNoFlyHandler())
             .setNext(new FlightCapacityHandler());
        chain.handle(this, passengerNames, flights);

        FlightOrder order = new FlightOrder(flights);
        order.setCustomer(this);
        order.setPrice(price);
        List<Passenger> passengers = passengerNames.stream()
                .map(Passenger::new)
                .collect(Collectors.toList());
        order.setPassengers(passengers);
        order.getScheduledFlights().forEach(sf -> sf.addPassengers(passengers));
        orders.add(order);
        return order;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

}
