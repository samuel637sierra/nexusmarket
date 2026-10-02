package application.domain.services;

import application.domain.cmd.AddProductToOrderCommand;
import application.domain.cmd.CancelOrderCommand;
import application.domain.cmd.ConfirmOrderCommand;
import application.domain.cmd.CreateOrderCommand;
import application.domain.cmd.ProcessPaymentCommand;
import application.domain.models.Customer;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.NotificationPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PaymentGatewayPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valueObjects.OrderStatus;

import java.util.Objects;

/**
 * Casos de uso del ciclo de vida de pedidos.
 */
public class OrderServiceImpl implements OrderService {

    private final OrderRepositoryPort orders;
    private final CustomerRepositoryPort customers;
    private final ProductRepositoryPort products;
    private final PaymentGatewayPort paymentGateway;
    private final NotificationPort notifications;

    public OrderServiceImpl(OrderRepositoryPort orders,
                            CustomerRepositoryPort customers,
                            ProductRepositoryPort products,
                            PaymentGatewayPort paymentGateway,
                            NotificationPort notifications) {
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
        this.paymentGateway = paymentGateway;
        this.notifications = notifications;
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        String customerId = requireText(command.customerId(), "El identificador del cliente es obligatorio");
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + customerId));
        if (!customer.isActive()) {
            throw new IllegalStateException("El cliente no está activo");
        }
        return orders.save(customer.createOrder());
    }

    @Override
    public void addProduct(AddProductToOrderCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Order order = getOrder(command.orderCode());
        String productCode = requireText(command.productCode(), "El código del producto es obligatorio");
        Product product = products.findByCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        order.addProduct(product, command.quantity());
        orders.save(order);
    }

    @Override
    public void confirmOrder(ConfirmOrderCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Order order = getOrder(command.orderCode());
        order.confirmOrder();
        orders.save(order);
        if (notifications != null) {
            notifications.sendOrderConfirmation(order);
        }
    }

    @Override
    public void cancelOrder(CancelOrderCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Order order = getOrder(command.orderCode());
        if (order.getStatus() == OrderStatus.PAID) {
            if (paymentGateway == null || !paymentGateway.refundPayment(order)) {
                throw new IllegalStateException("No se puede cancelar un pedido pagado sin reembolso confirmado");
            }
        }
        order.cancelOrder();
        orders.save(order);
    }

    @Override
    public boolean processPayment(ProcessPaymentCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Order order = getOrder(command.orderCode());
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.COMPLETED) {
            // Reintentar una operación ya cobrada no debe cobrar el pedido dos veces.
            return true;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("El pedido debe estar confirmado y pendiente de pago");
        }
        if (paymentGateway == null || !paymentGateway.processPayment(order)) {
            return false;
        }
        order.markAsPaid();
        if (!order.requiresShipping()) {
            order.completeDigitalOrder();
        }
        orders.save(order);
        return true;
    }

    @Override
    public void completeOrder(String orderCode) {
        Order order = getOrder(orderCode);
        order.completeOrder();
        orders.save(order);
    }

    @Override
    public Order getOrder(String orderCode) {
        String code = requireText(orderCode, "El código del pedido es obligatorio");
        return orders.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + code));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}