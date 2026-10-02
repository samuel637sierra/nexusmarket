package application.domain.valueObjects;

/**
 * Estados válidos del ciclo de vida de un pedido.
 */
public enum OrderStatus {
    CART,
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}