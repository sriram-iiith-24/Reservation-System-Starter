package flight.reservation.payment;

public class CreditCardPayment implements PaymentStrategy {
    private final CreditCard card;

    public CreditCardPayment(CreditCard card) {
        if (card == null || !card.isValid()) {
            throw new IllegalStateException("Payment information is not set or not valid.");
        }
        this.card = card;
    }

    @Override
    public boolean pay(double amount) {
        double remaining = card.getAmount() - amount;
        if (remaining < 0) {
            System.out.printf("Card limit reached - Balance: %f%n", remaining);
            throw new IllegalStateException("Card limit reached");
        }
        System.out.println("Paying " + amount + " using Credit Card.");
        card.setAmount(remaining);
        return true;
    }
}
