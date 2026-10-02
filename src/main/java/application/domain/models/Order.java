package application.domain.models;

import application.domain.valueObjects.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pedido realizado por un cliente.
 */
public class Order {

    private String orderCode;
    private Customer customer;
    private LocalDate orderDate;
    private double totalAmount;
    private OrderStatus status;
    private final Map<Product, Integer> products = new LinkedHashMap<>();
    private final Map<OrderStatus, java.util.Set<OrderStatus>> allowedTransitions =
            new EnumMap<>(OrderStatus.class);

    public Order() {
        this.orderDate = LocalDate.now();
        this.status = OrderStatus.CART;
        configureTransitions();
    }

    public Order(String orderCode, Customer customer) {
        this();
        this.orderCode = requireText(orderCode, "El código del pedido es obligatorio");
        this.customer = Objects.requireNonNull(customer, "El cliente es obligatorio");
    }

    public Order(String orderCode, Customer customer, LocalDate orderDate) {
        this(orderCode, customer);
        setOrderDate(orderDate);
    }

    private void configureTransitions() {
        allowedTransitions.put(OrderStatus.CART,
                java.util.Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        allowedTransitions.put(OrderStatus.PENDING_PAYMENT,
                java.util.Set.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        allowedTransitions.put(OrderStatus.PAID,
                java.util.Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        allowedTransitions.put(OrderStatus.SHIPPED,
                java.util.Set.of(OrderStatus.DELIVERED));
        allowedTransitions.put(OrderStatus.DELIVERED,
                java.util.Set.of(OrderStatus.COMPLETED));
        allowedTransitions.put(OrderStatus.COMPLETED, java.util.Set.of());
        allowedTransitions.put(OrderStatus.CANCELLED, java.util.Set.of());
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = requireText(orderCode, "El código del pedido es obligatorio");
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = Objects.requireNonNull(customer, "El cliente es obligatorio");
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = Objects.requireNonNull(orderDate, "La fecha del pedido es obligatoria");
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = requireAmount(totalAmount);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = Objects.requireNonNull(status, "El estado del pedido es obligatorio");
    }

    public void addProduct(Product product, int quantity) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        product.validateForSale();
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (status != OrderStatus.CART && status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("No se pueden modificar productos en este estado");
        }
        int newQuantity = Math.addExact(products.getOrDefault(product, 0), quantity);
        products.put(product, newQuantity);
        totalAmount = calculateTotal();
    }

    public void removeProduct(Product product) {
        if (product == null) {
            return;
        }
        if (status != OrderStatus.CART && status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("No se pueden modificar productos en este estado");
        }
        products.remove(product);
        totalAmount = calculateTotal();
    }

    public double calculateTotal() {
        BigDecimal total = products.entrySet().stream()
                .map(entry -> BigDecimal.valueOf(entry.getKey().getPrice())
                        .multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right))
                .setScale(2, RoundingMode.HALF_UP);
        totalAmount = requireAmount(total.doubleValue());
        return totalAmount;
    }

    public void confirmOrder() {
        if (products.isEmpty()) {
            throw new IllegalStateException("No se puede confirmar un pedido vacío");
        }
        if (status != OrderStatus.CART && status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("El pedido no puede confirmarse en su estado actual");
        }
        products.keySet().forEach(product -> product.validateForSale());
        calculateTotal();
        status = OrderStatus.PENDING_PAYMENT;
    }

    public void updateStatus(OrderStatus status) {
        Objects.requireNonNull(status, "El estado es obligatorio");
        if (this.status == status) {
            return;
        }
        if (!allowedTransitions.getOrDefault(this.status, java.util.Set.of()).contains(status)) {
            throw new IllegalStateException("Transición de estado no permitida: "
                    + this.status + " -> " + status);
        }
        this.status = status;
    }

    public void cancelOrder() {
        updateStatus(OrderStatus.CANCELLED);
    }

    public void markAsPaid() {
        updateStatus(OrderStatus.PAID);
    }

    public void markAsShipped() {
        updateStatus(OrderStatus.SHIPPED);
    }

    public void markAsDelivered() {
        updateStatus(OrderStatus.DELIVERED);
    }

    public void completeOrder() {
        updateStatus(OrderStatus.COMPLETED);
    }

    public void completeDigitalOrder() {
        if (requiresShipping()) {
            throw new IllegalStateException("Un pedido con productos físicos no se completa digitalmente");
        }
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("El pedido debe estar pagado para completarse");
        }
        status = OrderStatus.COMPLETED;
    }

    public boolean requiresShipping() {
        return products.keySet().stream().anyMatch(product -> product.requiresShipping());
    }

    public Map<Product, Integer> getProducts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(products));
    }

    public Map<Product, Integer> getItems() {
        return getProducts();
    }

    public int getProductQuantity(Product product) {
        return products.getOrDefault(product, 0);
    }

    public boolean isEmpty() {
        return products.isEmpty();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static double requireAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("El total debe ser un número finito no negativo");
        }
        return amount;
    }
}