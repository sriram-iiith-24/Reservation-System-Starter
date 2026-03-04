package flight.reservation.order;

public class BookingConfirmationObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(FlightOrder order) {
        System.out.printf("Booking confirmed for customer '%s'. Order ID: %s, Amount paid: %.2f%n",
                order.getCustomer().getName(),
                order.getId(),
                order.getPrice());
    }
}
