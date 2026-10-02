package application.domain.services;

import application.domain.cmd.AddProductToOrderCommand;
import application.domain.cmd.CancelOrderCommand;
import application.domain.cmd.ConfirmOrderCommand;
import application.domain.cmd.CreateOrderCommand;
import application.domain.cmd.ProcessPaymentCommand;
import application.domain.models.Order;
import application.domain.ports.in.OrderInputPort;

public interface OrderService extends OrderInputPort {
    Order createOrder(CreateOrderCommand command);
    void addProduct(AddProductToOrderCommand command);
    void confirmOrder(ConfirmOrderCommand command);
    void cancelOrder(CancelOrderCommand command);
    boolean processPayment(ProcessPaymentCommand command);
    void completeOrder(String orderCode);
    Order getOrder(String orderCode);
}
