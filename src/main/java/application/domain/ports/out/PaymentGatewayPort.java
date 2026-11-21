package application.domain.ports.out;

import application.domain.models.Order;

public interface PaymentGatewayPort {
    boolean processPayment(Order order);
    boolean refundPayment(Order order);

    /** Reembolso parcial de un pedido ya cobrado (devolución de un ítem). */
    boolean refundPartial(Order order, double amount);
}