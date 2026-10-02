package application.domain.models;

import application.domain.valueObjects.SystemRole;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Comprador de NexusMarket.
 */
public class Customer extends User {

    private String mainAddress;
    private final Map<Product, Integer> cart = new LinkedHashMap<>();
    private final List<Order> orderHistory = new java.util.ArrayList<>();

    public Customer() {
        setRole(SystemRole.CUSTOMER);
        setStatus(application.domain.valueObjects.UserStatus.ACTIVE);
    }

    public Customer(long userId, String username, String password, String personId,
                    String fullName, String email, String phoneNumber, String address,
                    LocalDate birthDate, String mainAddress) {
        super(userId, username, password, SystemRole.CUSTOMER,
                application.domain.valueObjects.UserStatus.ACTIVE,
                personId, fullName, email, phoneNumber, address, birthDate);
        this.mainAddress = mainAddress;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = requireText(mainAddress, "La dirección principal es obligatoria");
    }

    public Order createOrder() {
        String code = "ORD-" + UUID.randomUUID();
        Order order = new Order(code, this);
        orderHistory.add(order);
        return order;
    }

    public void addProductToCart(Product product, int quantity) {
        product.validateForSale();
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        cart.put(product, Math.addExact(cart.getOrDefault(product, 0), quantity));
    }

    public void removeProductFromCart(Product product) {
        cart.remove(product);
    }

    public List<Order> viewOrderHistory() {
        return List.copyOf(orderHistory);
    }

    public void updateMainAddress(String address) {
        setMainAddress(address);
        setAddress(address);
    }

    public Map<Product, Integer> getCart() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(cart));
    }

    public List<Order> getOrderHistory() {
        return viewOrderHistory();
    }

    public boolean hasProductInCart(Product product) {
        return cart.containsKey(product);
    }

    public void clearCart() {
        cart.clear();
    }

    public void addOrderToHistory(Order order) {
        Objects.requireNonNull(order, "El pedido es obligatorio");
        if (order.getCustomer() != this) {
            throw new IllegalArgumentException("El pedido no pertenece al cliente");
        }
        orderHistory.add(order);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}