package application.domain.services;

import application.domain.models.Customer;
import application.domain.models.Order;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueObjects.OrderStatus;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerOrderSummaryServiceImpl implements CustomerOrderSummaryService {
    private static final java.util.Set<OrderStatus> REVENUE_STATUSES = java.util.Set.of(
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final CustomerRepositoryPort customers;
    private final OrderRepositoryPort orders;

    public CustomerOrderSummaryServiceImpl(CustomerRepositoryPort customers, OrderRepositoryPort orders) {
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
    }

    @Override
    public Summary summarize(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }
        Customer customer = customers.findById(customerId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + customerId));
        var customerOrders = orders.findByCustomer(customer);
        BigDecimal lifetimeSpend = BigDecimal.ZERO;
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (Order order : customerOrders) {
            if (order == null) {
            continue;
            }
            if (REVENUE_STATUSES.contains(order.getStatus())) {
            lifetimeSpend = lifetimeSpend.add(BigDecimal.valueOf(order.getTotalAmount()));
            }
            OrderStatus status = order.getStatus();
            Long existingCount = byStatus.get(status);
            byStatus.put(status, existingCount == null ? 1L : existingCount.longValue() + 1L);
        }
        return new Summary(customerOrders.size(), lifetimeSpend, Map.copyOf(byStatus));
    }
}