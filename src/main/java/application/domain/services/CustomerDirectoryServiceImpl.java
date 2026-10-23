package application.domain.services;

import application.domain.models.Customer;
import application.domain.ports.out.CustomerRepositoryPort;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerDirectoryServiceImpl implements CustomerDirectoryService {
    private final CustomerRepositoryPort customers;

    public CustomerDirectoryServiceImpl(CustomerRepositoryPort customers) {
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
    }

    @Override
    public Customer findById(String customerId) {
        String id = requireText(customerId, "El identificador del cliente es obligatorio");
        return customers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(customers.findAll());
    }

    @Override
    public void delete(String customerId) {
        Customer customer = findById(customerId);
        customers.delete(customer.getPersonId());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}