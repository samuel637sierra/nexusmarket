package application.api;

import application.domain.models.Customer;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ApiViews {
    private ApiViews() {
    }

    public record UserView(String personId, long userId, String username, String fullName,
                           String email, String role, String status, String sellerCode) {
        public static UserView from(User user) {
            String sellerCode = user instanceof application.domain.models.Seller seller
                    ? seller.getSellerCode() : null;
            return new UserView(user.getPersonId(), user.getUserId(), user.getUsername(),
                    user.getFullName(), user.getEmail(), user.getRole().name(),
                    user.getStatus().name(), sellerCode);
        }
    }

    public record ProductView(String productCode, String name, String description,
                              double price, String type, String status) {
        public static ProductView from(Product product) {
            return new ProductView(product.getProductCode(), product.getName(), product.getDescription(),
                    product.getPrice(), product.getType().name(), product.getStatus().name());
        }
    }

    public record CartItemView(String productCode, String name, double unitPrice, int quantity) {
        public static CartItemView from(Product product, int quantity) {
            return new CartItemView(product.getProductCode(), product.getName(), product.getPrice(), quantity);
        }
    }

    public record CartView(String customerId, List<CartItemView> items, double total) {
        public static CartView from(Customer customer) {
            List<CartItemView> items = customer.getCart().entrySet().stream()
                    .map(entry -> CartItemView.from(entry.getKey(), entry.getValue()))
                    .toList();
            double total = items.stream().mapToDouble(item -> item.unitPrice() * item.quantity()).sum();
            return new CartView(customer.getPersonId(), items, total);
        }
    }

    public record OrderView(String orderCode, String customerId, LocalDate orderDate,
                            double total, String status, boolean requiresShipping,
                            Map<String, Integer> items) {
        public static OrderView from(Order order) {
            Map<String, Integer> items = new LinkedHashMap<>();
            for (Map.Entry<Product, Integer> item : order.getProducts().entrySet()) {
                Product product = Objects.requireNonNull(item.getKey(), "El producto del pedido es obligatorio");
                Integer quantity = Objects.requireNonNull(item.getValue(), "La cantidad del pedido es obligatoria");
                items.put(product.getProductCode(), quantity.intValue());
            }
            return new OrderView(order.getOrderCode(),
                    order.getCustomer() == null ? null : order.getCustomer().getPersonId(),
                    order.getOrderDate(), order.getTotalAmount(), order.getStatus().name(),
                    order.requiresShipping(), Map.copyOf(items));
        }
    }

    public record ShipmentView(String shipmentCode, String orderCode, String status,
                               String originWarehouse, String destinationAddress,
                               LocalDate shippingDate, LocalDate deliveryDate) {
        public static ShipmentView from(Shipment shipment) {
            return new ShipmentView(shipment.getShipmentCode(), shipment.getOrder().getOrderCode(),
                    shipment.getStatus(), shipment.getOriginWarehouse().getWarehouseCode(),
                    shipment.getDestinationAddress(), shipment.getShippingDate(), shipment.getDeliveryDate());
        }
    }

    public record InvoiceView(String invoiceNumber, String orderCode, LocalDate issueDate,
                              double total, boolean sentByEmail) {
        public static InvoiceView from(Invoice invoice) {
            return new InvoiceView(invoice.getInvoiceNumber(),
                    invoice.getOrder() == null ? null : invoice.getOrder().getOrderCode(),
                    invoice.getIssueDate(), invoice.getTotal(), invoice.isSentByEmail());
        }
    }

    public record WarehouseView(String warehouseCode, String name, String address, int capacity) {
        public static WarehouseView from(Warehouse warehouse) {
            return new WarehouseView(warehouse.getWarehouseCode(), warehouse.getName(),
                    warehouse.getAddress(), warehouse.getCapacity());
        }
    }

    public record ErrorView(String error, String message) {
    }
}