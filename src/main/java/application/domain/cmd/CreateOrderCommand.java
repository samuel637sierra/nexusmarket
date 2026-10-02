package application.domain.cmd;

/** Entrada del caso de uso de creación de pedidos. */
public record CreateOrderCommand(String customerId) {
}