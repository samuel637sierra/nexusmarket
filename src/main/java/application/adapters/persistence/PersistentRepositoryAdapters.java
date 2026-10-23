package application.adapters.persistence;

import application.domain.models.Customer;
import application.domain.models.Inventory;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PersistentRepositoryAdapters {
    private PersistentRepositoryAdapters() {
    }

    public static final class CustomerRepositoryAdapter implements CustomerRepositoryPort {
        private static final String BUCKET = "customer";
        private final ObjectStore store;

        public CustomerRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Customer save(Customer customer) {
            Objects.requireNonNull(customer, "El cliente es obligatorio");
            return store.save(BUCKET, key(customer.getPersonId()), customer);
        }

        @Override
        public Optional<Customer> findById(String customerId) {
            return store.find(BUCKET, customerId, Customer.class);
        }

        @Override
        public List<Customer> findAll() {
            return store.findAll(BUCKET, Customer.class);
        }

        @Override
        public void delete(String customerId) {
            store.delete(BUCKET, customerId);
        }
    }

    public static final class UserRepositoryAdapter implements UserRepositoryPort {
        private static final String BUCKET = "user";
        private final ObjectStore store;

        public UserRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public synchronized User save(User user) {
            Objects.requireNonNull(user, "El usuario es obligatorio");
            boolean duplicate = findAll().stream()
                    .anyMatch(existing -> existing.getUserId() != user.getUserId()
                            && existing.getUsername().equalsIgnoreCase(user.getUsername()));
            if (duplicate) {
                throw new IllegalStateException("El nombre de usuario ya está registrado");
            }
            return store.save(BUCKET, Long.toString(user.getUserId()), user);
        }

        @Override
        public Optional<User> findById(Long userId) {
            return userId == null ? Optional.empty()
                    : store.find(BUCKET, Long.toString(userId), User.class);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            return findAll().stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return store.findAll(BUCKET, User.class);
        }

        @Override
        public void delete(Long userId) {
            if (userId != null) {
                store.delete(BUCKET, Long.toString(userId));
            }
        }
    }

    public static final class SellerRepositoryAdapter implements SellerRepositoryPort {
        private static final String BUCKET = "seller";
        private final ObjectStore store;

        public SellerRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Seller save(Seller seller) {
            Objects.requireNonNull(seller, "El vendedor es obligatorio");
            return store.save(BUCKET, key(seller.getSellerCode()), seller);
        }

        @Override
        public Optional<Seller> findBySellerCode(String sellerCode) {
            return store.find(BUCKET, sellerCode, Seller.class);
        }

        @Override
        public List<Seller> findAll() {
            return store.findAll(BUCKET, Seller.class);
        }

        @Override
        public void delete(String sellerCode) {
            store.delete(BUCKET, sellerCode);
        }
    }

    public static final class ProductRepositoryAdapter implements ProductRepositoryPort {
        private static final String BUCKET = "product";
        private final ObjectStore store;

        public ProductRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Product save(Product product) {
            Objects.requireNonNull(product, "El producto es obligatorio");
            return store.save(BUCKET, key(product.getProductCode()), product);
        }

        @Override
        public Optional<Product> findByCode(String productCode) {
            return store.find(BUCKET, productCode, Product.class);
        }

        @Override
        public List<Product> findAll() {
            return store.findAll(BUCKET, Product.class);
        }

        @Override
        public List<Product> findPublishedProducts() {
            List<Product> published = new java.util.ArrayList<>();
            for (Product product : findAll()) {
                if (product != null && product.isPublished()) {
                    published.add(product);
                }
            }
            return List.copyOf(published);
        }

        @Override
        public void delete(String productCode) {
            store.delete(BUCKET, productCode);
        }
    }

    public static final class OrderRepositoryAdapter implements OrderRepositoryPort {
        private static final String BUCKET = "order";
        private final ObjectStore store;

        public OrderRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Order save(Order order) {
            Objects.requireNonNull(order, "El pedido es obligatorio");
            return store.save(BUCKET, key(order.getOrderCode()), order);
        }

        @Override
        public Optional<Order> findByCode(String orderCode) {
            return store.find(BUCKET, orderCode, Order.class);
        }

        @Override
        public List<Order> findByCustomer(Customer customer) {
            Objects.requireNonNull(customer, "El cliente es obligatorio");
            return findAll().stream()
                    .filter(order -> order.getCustomer() != null
                            && Objects.equals(order.getCustomer().getPersonId(), customer.getPersonId()))
                    .toList();
        }

        @Override
        public List<Order> findAll() {
            return store.findAll(BUCKET, Order.class);
        }

        @Override
        public void delete(String orderCode) {
            store.delete(BUCKET, orderCode);
        }
    }

    public static final class WarehouseRepositoryAdapter implements WarehouseRepositoryPort {
        private static final String BUCKET = "warehouse";
        private final ObjectStore store;

        public WarehouseRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Warehouse save(Warehouse warehouse) {
            Objects.requireNonNull(warehouse, "La bodega es obligatoria");
            return store.save(BUCKET, key(warehouse.getWarehouseCode()), warehouse);
        }

        @Override
        public Optional<Warehouse> findByCode(String warehouseCode) {
            return store.find(BUCKET, warehouseCode, Warehouse.class);
        }

        @Override
        public List<Warehouse> findAll() {
            return store.findAll(BUCKET, Warehouse.class);
        }

        @Override
        public void delete(String warehouseCode) {
            store.delete(BUCKET, warehouseCode);
        }
    }

    public static final class InventoryRepositoryAdapter implements InventoryRepositoryPort {
        private static final String BUCKET = "inventory";
        private final ObjectStore store;

        public InventoryRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Inventory save(Inventory inventory) {
            Objects.requireNonNull(inventory, "El inventario es obligatorio");
            return store.save(BUCKET, key(inventory.getInventoryCode()), inventory);
        }

        @Override
        public Optional<Inventory> findByProduct(Product product) {
            Objects.requireNonNull(product, "El producto es obligatorio");
            return findAll().stream()
                    .filter(inventory -> sameProduct(inventory.getProduct(), product))
                    .findFirst();
        }

        @Override
        public Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse) {
            Objects.requireNonNull(product, "El producto es obligatorio");
            Objects.requireNonNull(warehouse, "La bodega es obligatoria");
            return findAll().stream()
                    .filter(inventory -> sameProduct(inventory.getProduct(), product))
                    .filter(inventory -> inventory.getWarehouse() != null
                            && Objects.equals(inventory.getWarehouse().getWarehouseCode(), warehouse.getWarehouseCode()))
                    .findFirst();
        }

        @Override
        public synchronized void updateStock(Product product, int quantity) {
            Inventory inventory = findByProduct(product)
                    .orElseThrow(() -> new IllegalArgumentException("No existe inventario para el producto"));
            inventory.setQuantity(quantity);
            save(inventory);
        }

        @Override
        public List<Inventory> findAll() {
            return store.findAll(BUCKET, Inventory.class);
        }

        private static boolean sameProduct(Product first, Product second) {
            return first != null && second != null
                    && Objects.equals(first.getProductCode(), second.getProductCode());
        }
    }

    public static final class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {
        private static final String BUCKET = "invoice";
        private final ObjectStore store;

        public InvoiceRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Invoice save(Invoice invoice) {
            Objects.requireNonNull(invoice, "La factura es obligatoria");
            return store.save(BUCKET, key(invoice.getInvoiceNumber()), invoice);
        }

        @Override
        public Optional<Invoice> findByNumber(String invoiceNumber) {
            return store.find(BUCKET, invoiceNumber, Invoice.class);
        }

        @Override
        public List<Invoice> findAll() {
            return store.findAll(BUCKET, Invoice.class);
        }
    }

    public static final class ShipmentRepositoryAdapter implements ShipmentRepositoryPort {
        private static final String BUCKET = "shipment";
        private final ObjectStore store;

        public ShipmentRepositoryAdapter(ObjectStore store) {
            this.store = Objects.requireNonNull(store);
        }

        @Override
        public Shipment save(Shipment shipment) {
            Objects.requireNonNull(shipment, "El envío es obligatorio");
            return store.save(BUCKET, key(shipment.getShipmentCode()), shipment);
        }

        @Override
        public Optional<Shipment> findByCode(String shipmentCode) {
            return store.find(BUCKET, shipmentCode, Shipment.class);
        }

        @Override
        public List<Shipment> findAll() {
            return store.findAll(BUCKET, Shipment.class);
        }

        @Override
        public void updateStatus(String shipmentCode, String status) {
            Shipment shipment = findByCode(shipmentCode)
                    .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado: " + shipmentCode));
            shipment.updateStatus(status);
            save(shipment);
        }
    }

    private static String key(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La clave del objeto es obligatoria");
        }
        return value.trim();
    }
}
