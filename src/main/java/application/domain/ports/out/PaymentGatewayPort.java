package application.domain.ports.out;

import application.domain.models.Order;

public interface PaymentGatewayPort {
    boolean processPayment(Order order);
    boolean refundPayment(Order order);
}