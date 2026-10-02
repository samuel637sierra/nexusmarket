package application.domain.ports.out;

import application.domain.models.Invoice;
import application.domain.models.Order;

public interface NotificationPort {
    void sendEmail(String email, String message);
    void sendOrderConfirmation(Order order);
    void sendInvoice(Invoice invoice);
}