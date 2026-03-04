package flight.reservation.order;

import flight.reservation.flight.ScheduledFlight;

public class CapacityMonitorObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(FlightOrder order) {
        for (ScheduledFlight sf : order.getScheduledFlights()) {
            int capacity = sf.getCapacity();
            int available = sf.getAvailableCapacity();
            int taken = capacity - available;
            double fillRate = (double) taken / capacity * 100;
            if (fillRate >= 80) {
                System.out.printf("Capacity alert: Flight %d is %.0f%% full (%d/%d seats taken).%n",
                        sf.getNumber(), fillRate, taken, capacity);
            }
        }
    }
}
