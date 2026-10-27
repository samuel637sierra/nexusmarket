package application.domain.valueObjects;

/**
 * Estados válidos del ciclo de vida de un pedido. CANCELLED representa
 * la cancelación antes del despacho; no se permite cancelar un pedido enviado.
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
