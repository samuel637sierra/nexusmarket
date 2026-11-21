package application.adapters.memory;

import application.domain.models.Invoice;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.ports.out.InvoiceGeneratorPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.NotificationPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.PaymentGatewayPort;
import application.domain.ports.out.ReportGeneratorPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.ShipmentTrackingPort;
import application.domain.valueObjects.OrderStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryInfrastructureAdapters {
    private InMemoryInfrastructureAdapters() {
    }

    public static final class PasswordServiceAdapter implements PasswordServicePort {
        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        @Override
        public String hash(String plainPassword) {
            if (plainPassword == null || plainPassword.isBlank()) {
                throw new IllegalArgumentException("La contraseña no puede estar vacía");
            }
            return encoder.encode(plainPassword);
        }

        @Override
        public boolean matches(String plainPassword, String storedPassword) {
            return plainPassword != null && storedPassword != null
                    && encoder.matches(plainPassword, storedPassword);
        }
    }

    public static final class SandboxPaymentGatewayAdapter implements PaymentGatewayPort {
        @Override
        public boolean processPayment(Order order) {
            return order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT
                    && Double.isFinite(order.getTotalAmount()) && order.getTotalAmount() > 0;
        }

        @Override
        public boolean refundPayment(Order order) {
            return order != null && order.getStatus() == OrderStatus.PAID;
        }

        @Override
        // Se invoca con la devolución ya registrada en el pedido: lo reembolsado no puede superar lo cobrado.
        public boolean refundPartial(Order order, double amount) {
            return order != null && amount > 0 && order.getRefundedAmount() <= order.getTotalAmount()
                    && (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED);
        }
    }

    public static final class ShipmentTrackingAdapter implements ShipmentTrackingPort {
        private final ConcurrentMap<String, String> statuses = new ConcurrentHashMap<>();

        @Override
        public void registerShipment(Shipment shipment) {
            Objects.requireNonNull(shipment, "El envío es obligatorio");
            statuses.put(shipment.getShipmentCode(), shipment.getStatus());
        }

        @Override
        public String getShipmentStatus(String shipmentCode) {
            String status = statuses.get(shipmentCode);
            if (status == null) {
                throw new IllegalArgumentException("Seguimiento no encontrado: " + shipmentCode);
            }
            return status;
        }

        @Override
        public void confirmDelivery(String shipmentCode) {
            statuses.compute(shipmentCode, (code, status) -> {
                if (status == null) {
                    throw new IllegalArgumentException("Seguimiento no encontrado: " + code);
                }
                return Shipment.DELIVERED;
            });
        }
    }

    public static final class NotificationAdapter implements NotificationPort {
        private final List<NotificationEvent> events = new ArrayList<>();

        @Override
        public synchronized void sendEmail(String email, String message) {
            events.add(new NotificationEvent(email, message));
        }

        @Override
        public synchronized void sendOrderConfirmation(Order order) {
            Objects.requireNonNull(order, "El pedido es obligatorio");
            String email = order.getCustomer() == null ? null : order.getCustomer().getEmail();
            events.add(new NotificationEvent(email, "Confirmación del pedido " + order.getOrderCode()));
        }

        @Override
        public synchronized void sendInvoice(Invoice invoice) {
            Objects.requireNonNull(invoice, "La factura es obligatoria");
            String email = invoice.getOrder() == null || invoice.getOrder().getCustomer() == null
                    ? null : invoice.getOrder().getCustomer().getEmail();
            events.add(new NotificationEvent(email, "Factura " + invoice.getInvoiceNumber()));
        }

        public synchronized List<NotificationEvent> getEvents() {
            return List.copyOf(events);
        }
    }

    public record NotificationEvent(String recipient, String message) {
    }

    public static final class InvoiceGeneratorAdapter implements InvoiceGeneratorPort {
        @Override
        public Invoice generateInvoice(Order order) {
            return new Invoice(Objects.requireNonNull(order, "El pedido es obligatorio"));
        }

        @Override
        public byte[] exportPdf(Invoice invoice) {
            Objects.requireNonNull(invoice, "La factura es obligatoria");
            invoice.generateInvoice();
            String orderCode = invoice.getOrder() == null ? "" : invoice.getOrder().getOrderCode();
            String content = "BT\n/F1 12 Tf\n50 740 Td\n(NexusMarket invoice "
                    + pdfEscape(invoice.getInvoiceNumber()) + ") Tj\n0 -20 Td\n(Order: "
                    + pdfEscape(orderCode) + ") Tj\n0 -20 Td\n(Total: "
                    + String.format(Locale.ROOT, "%.2f", invoice.getTotal()) + ") Tj\nET\n";
            byte[] stream = content.getBytes(StandardCharsets.US_ASCII);
            List<String> objects = List.of(
                    "<< /Type /Catalog /Pages 2 0 R >>",
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                            + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                    "<< /Length " + stream.length + " >>\nstream\n" + content + "endstream");
            return buildPdf(objects);
        }

        private static byte[] buildPdf(List<String> objects) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            write(output, "%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int index = 0; index < objects.size(); index++) {
                offsets.add(output.size());
                write(output, (index + 1) + " 0 obj\n" + objects.get(index) + "\nendobj\n");
            }
            int xrefOffset = output.size();
            write(output, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
            for (int index = 1; index < offsets.size(); index++) {
                write(output, String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(index)));
            }
            write(output, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n"
                    + "startxref\n" + xrefOffset + "\n%%EOF\n");
            return output.toByteArray();
        }

        private static String pdfEscape(String value) {
            if (value == null) {
                return "";
            }
            return value.replaceAll("[^\\x20-\\x7E]", "?")
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }

        private static void write(ByteArrayOutputStream output, String value) {
            output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
        }
    }

    public static final class ReportGeneratorAdapter implements ReportGeneratorPort {
        private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(
                OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.COMPLETED);

        private final OrderRepositoryPort orders;
        private final InventoryRepositoryPort inventories;
        private final ShipmentRepositoryPort shipments;

        public ReportGeneratorAdapter(OrderRepositoryPort orders,
                                      InventoryRepositoryPort inventories,
                                      ShipmentRepositoryPort shipments) {
            this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
            this.inventories = Objects.requireNonNull(inventories, "El repositorio de inventario es obligatorio");
            this.shipments = Objects.requireNonNull(shipments, "El repositorio de envíos es obligatorio");
        }

        @Override
        public byte[] generateSalesReport() {
            StringBuilder csv = new StringBuilder("orderCode,status,total,refunded,net\n");
            orders.findAll().stream()
                    .filter(order -> REVENUE_STATUSES.contains(order.getStatus()))
                    .forEach(order -> csv.append(order.getOrderCode()).append(',')
                            .append(order.getStatus()).append(',')
                            .append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f", order.getTotalAmount(),
                                    order.getRefundedAmount(), order.getTotalAmount() - order.getRefundedAmount()))
                            .append('\n'));
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] generateInventoryReport() {
            StringBuilder csv = new StringBuilder("productCode,warehouseCode,quantity\n");
            inventories.findAll().stream()
                    .sorted((first, second) -> first.getInventoryCode().compareTo(second.getInventoryCode()))
                    .forEach(inventory -> appendInventory(csv, inventory));
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] generateShipmentReport() {
            StringBuilder csv = new StringBuilder("shipmentCode,orderCode,status,shippingDate,deliveryDate\n");
            shipments.findAll().stream()
                    .sorted((first, second) -> first.getShipmentCode().compareTo(second.getShipmentCode()))
                    .forEach(shipment -> csv.append(shipment.getShipmentCode()).append(',')
                            .append(shipment.getOrder().getOrderCode()).append(',')
                            .append(shipment.getStatus()).append(',')
                            .append(shipment.getShippingDate() == null ? "" : shipment.getShippingDate()).append(',')
                            .append(shipment.getDeliveryDate() == null ? "" : shipment.getDeliveryDate()).append('\n'));
            return csv.toString().getBytes(StandardCharsets.UTF_8);
        }

        private static void appendInventory(StringBuilder csv, Inventory inventory) {
            String productCode = inventory.getProduct() == null ? "" : inventory.getProduct().getProductCode();
            String warehouseCode = inventory.getWarehouse() == null ? "" : inventory.getWarehouse().getWarehouseCode();
            csv.append(productCode).append(',').append(warehouseCode).append(',')
                    .append(inventory.getQuantity()).append('\n');
        }
    }
}