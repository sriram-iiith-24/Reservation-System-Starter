package flight.reservation.order;

public interface BookingObserver {
    void onBookingConfirmed(FlightOrder order);
}
