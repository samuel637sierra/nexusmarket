package application.domain.services;

import application.domain.models.Customer;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.cmd.ConfirmOrderCommand;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CartCheckoutServiceImpl implements CartCheckoutService {
    private final CustomerRepositoryPort customers;
    private final OrderRepositoryPort orders;
    private final OrderValidationService validation;
    private final OrderService orderService;

    public CartCheckoutServiceImpl(CustomerRepositoryPort customers, OrderRepositoryPort orders,
                                   OrderValidationService validation, OrderService orderService) {
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
        this.validation = Objects.requireNonNull(validation, "El servicio de validación es obligatorio");
        this.orderService = Objects.requireNonNull(orderService, "El servicio de pedidos es obligatorio");
    }

    @Override
    public Order checkout(String customerId) {
        String id = requireText(customerId, "El identificador del cliente es obligatorio");
        Customer customer = customers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
        if (!customer.isActive()) {
            throw new IllegalStateException("El cliente no está activo");
        }
        Map<Product, Integer> cart = customer.getCart();
        if (cart.isEmpty()) {
            throw new IllegalStateException("No se puede finalizar un carrito vacío");
        }

        Order order = new Order("ORD-" + UUID.randomUUID(), customer);
        for (Map.Entry<Product, Integer> item : cart.entrySet()) {
            Product product = Objects.requireNonNull(item.getKey(), "El producto del carrito es obligatorio");
            Integer quantity = Objects.requireNonNull(item.getValue(), "La cantidad del carrito es obligatoria");
            order.addProduct(product, quantity.intValue());
        }
        orders.save(order);
        try {
            validation.validateForCheckout(order.getOrderCode());
            orderService.confirmOrder(new ConfirmOrderCommand(order.getOrderCode()));
        } catch (RuntimeException exception) {
            orders.delete(order.getOrderCode());
            throw exception;
        }
        Order savedOrder = orders.findByCode(order.getOrderCode())
            .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el pedido confirmado"));
        customer.addOrderToHistory(savedOrder);
        customer.clearCart();
        customers.save(customer);
        return savedOrder;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}