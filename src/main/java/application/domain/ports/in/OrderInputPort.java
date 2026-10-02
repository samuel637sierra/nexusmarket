package application.domain.ports.in;

import application.domain.cmd.AddProductToOrderCommand;
import application.domain.cmd.CancelOrderCommand;
import application.domain.cmd.ConfirmOrderCommand;
import application.domain.cmd.CreateOrderCommand;
import application.domain.models.Order;

/** Puerto de entrada para el ciclo de vida de pedidos. */
public interface OrderInputPort {

    Order createOrder(CreateOrderCommand command);

    void addProduct(AddProductToOrderCommand command);

    void confirmOrder(ConfirmOrderCommand command);

    void cancelOrder(CancelOrderCommand command);

    Order getOrder(String orderCode);
}