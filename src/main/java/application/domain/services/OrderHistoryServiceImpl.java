package application.domain.services;

import application.domain.models.Customer;
import application.domain.models.Order;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OrderHistoryServiceImpl implements OrderHistoryService {
    private final CustomerRepositoryPort customers;
    private final OrderRepositoryPort orders;

    public OrderHistoryServiceImpl(CustomerRepositoryPort customers, OrderRepositoryPort orders) {
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
    }

    @Override
    public List<Order> findByCustomer(String customerId) {
        String id = requireText(customerId, "El identificador del cliente es obligatorio");
        Customer customer = customers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
        return List.copyOf(orders.findByCustomer(customer));
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(orders.findAll());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}