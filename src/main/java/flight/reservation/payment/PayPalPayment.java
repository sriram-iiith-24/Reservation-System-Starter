package flight.reservation.payment;

public class PayPalPayment implements PaymentStrategy {
    private final String email;
    private final String password;

    public PayPalPayment(String email, String password) {
        if (email == null || password == null || !email.equals(Paypal.DATA_BASE.get(password))) {
            throw new IllegalStateException("Payment information is not set or not valid.");
        }
        this.email = email;
        this.password = password;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Paying " + amount + " using PayPal.");
        return true;
    }
}
