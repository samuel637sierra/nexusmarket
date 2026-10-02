package application.domain.cmd;

/** Entrada del caso de uso para agregar un producto a un pedido. */
public record AddProductToOrderCommand(
        String orderCode,
        String productCode,
        int quantity
) {
}