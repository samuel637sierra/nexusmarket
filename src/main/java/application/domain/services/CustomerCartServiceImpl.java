package application.domain.services;

import application.domain.models.Customer;
import application.domain.models.Product;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerCartServiceImpl implements CustomerCartService {
    private final CustomerRepositoryPort customers;
    private final ProductRepositoryPort products;
    private final ProductAvailabilityService availability;

    public CustomerCartServiceImpl(CustomerRepositoryPort customers, ProductRepositoryPort products,
                                   ProductAvailabilityService availability) {
        this.availability = Objects.requireNonNull(availability, "El servicio de disponibilidad es obligatorio");
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
    }

    @Override
    public Map<Product, Integer> viewCart(String customerId) {
        return findCustomer(customerId).getCart();
    }

    @Override
    public void addProduct(String customerId, String productCode, int quantity) {
        Customer customer = findCustomer(customerId);
        Product product = products.findByCode(requireText(productCode, "El código del producto es obligatorio"))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        if (product.requiresShipping() && quantity > 0) {
            int requested = Math.addExact(customer.getCart().getOrDefault(product, 0), quantity);
            int available = availability.getTotalAvailable(product.getProductCode());
            if (requested > available) {
                throw new IllegalStateException("Stock insuficiente para " + product.getName()
                        + ": solicitadas " + requested + ", disponibles " + available);
            }
        }
        customer.addProductToCart(product, quantity);
        customers.save(customer);
    }

    @Override
    public void removeProduct(String customerId, String productCode) {
        Customer customer = findCustomer(customerId);
        Product product = products.findByCode(requireText(productCode, "El código del producto es obligatorio"))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        customer.removeProductFromCart(product);
        customers.save(customer);
    }

    @Override
    public void clearCart(String customerId) {
        Customer customer = findCustomer(customerId);
        customer.clearCart();
        customers.save(customer);
    }

    @Override
    public Customer findCustomer(String customerId) {
        String id = requireText(customerId, "El identificador del cliente es obligatorio");
        return customers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}