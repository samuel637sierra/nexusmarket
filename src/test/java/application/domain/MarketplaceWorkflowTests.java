package application.domain;

import application.domain.cmd.CreateProductCommand;
import application.domain.cmd.CreateShipmentCommand;
import application.domain.cmd.DispatchShipmentCommand;
import application.domain.cmd.GenerateInvoiceCommand;
import application.domain.cmd.ProcessPaymentCommand;
import application.domain.cmd.RegisterCustomerCommand;
import application.domain.cmd.RegisterSellerCommand;
import application.domain.models.Customer;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.adapters.persistence.ObjectStore;
import application.adapters.persistence.PersistentRepositoryAdapters;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.ShipmentTrackingPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.CartCheckoutService;
import application.domain.services.CustomerCartService;
import application.domain.services.InventoryService;
import application.domain.services.InvoiceService;
import application.domain.services.OrderService;
import application.domain.services.ProductService;
import application.domain.services.ShipmentService;
import application.domain.services.UserManagementService;
import application.domain.valueObjects.OrderStatus;
import application.domain.valueObjects.ProductType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "nexusmarket.tax.rate=0.19")
class MarketplaceWorkflowTests {
    @Autowired
    private UserManagementService users;
    @Autowired
    private ProductService products;
    @Autowired
    private CustomerCartService carts;
    @Autowired
    private CartCheckoutService checkout;
    @Autowired
    private OrderService orders;
    @Autowired
    private InventoryService inventory;
    @Autowired
    private ShipmentService shipments;
    @Autowired
    private InvoiceService invoices;
    @Autowired
    private WarehouseRepositoryPort warehouses;
        @Autowired
        private PasswordServicePort passwords;
        @Autowired
        private ShipmentTrackingPort tracking;
        @Autowired
        private ObjectStore objectStore;

    @Test
    void completesPhysicalPurchaseAndInvoiceFlow() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Seller seller = users.registerSeller(new RegisterSellerCommand(
                "Vendedor " + suffix, "seller-" + suffix + "@example.com", "3001234567",
                "Comercio " + suffix, 10000000L + Math.abs(suffix.hashCode()),
                "seller-" + suffix, "seller-password-123", "Calle 10"));
        Product product = products.createProduct(new CreateProductCommand(
                "Producto " + suffix, "Producto de prueba", 125.50, ProductType.PHYSICAL,
                seller.getSellerCode()));
        products.publishProduct(product.getProductCode());

        Warehouse warehouse = warehouses.save(new Warehouse(
                "WH-" + suffix, "Bodega de prueba", "Calle 20", 100));
        Customer customer = users.registerCustomer(new RegisterCustomerCommand(
                "Cliente " + suffix, "customer-" + suffix + "@example.com", "3009876543",
                "Carrera 1", "customer-" + suffix, "customer-password-123"));
        assertNotEquals("customer-password-123", customer.getPassword());
        assertTrue(passwords.matches("customer-password-123", customer.getPassword()));
        carts.addProduct(customer.getPersonId(), product.getProductCode(), 2);
        assertThrows(IllegalStateException.class, () -> checkout.checkout(customer.getPersonId()));
        inventory.addStock(new application.domain.cmd.AddStockCommand(
                product.getProductCode(), warehouse.getWarehouseCode(), 5));
        Order order = checkout.checkout(customer.getPersonId());

        assertEquals(251.0, order.getTotalAmount(), 0.001);
        Order restoredOrder = new PersistentRepositoryAdapters.OrderRepositoryAdapter(objectStore)
                .findByCode(order.getOrderCode()).orElseThrow();
        assertEquals(order.getTotalAmount(), restoredOrder.getTotalAmount(), 0.001);
        assertEquals(2, restoredOrder.getProducts().values().iterator().next());
        assertTrue(orders.processPayment(new ProcessPaymentCommand(order.getOrderCode())));

        Shipment shipment = shipments.createShipment(new CreateShipmentCommand(
                order.getOrderCode(), warehouse.getWarehouseCode()));
        shipments.dispatchShipment(new DispatchShipmentCommand(shipment.getShipmentCode()));
        assertTrue(shipments.trackShipment(shipment.getShipmentCode()).contains(Shipment.IN_TRANSIT));
        assertEquals(Shipment.IN_TRANSIT, tracking.getShipmentStatus(shipment.getShipmentCode()));
        assertEquals(3, inventory.getAvailableStock(product.getProductCode(), warehouse.getWarehouseCode()));

        shipments.deliverShipment(new application.domain.cmd.DeliverShipmentCommand(shipment.getShipmentCode()));
        assertTrue(shipments.trackShipment(shipment.getShipmentCode()).contains(Shipment.DELIVERED));
        assertEquals(Shipment.DELIVERED, tracking.getShipmentStatus(shipment.getShipmentCode()));
        assertEquals(OrderStatus.DELIVERED, orders.getOrder(order.getOrderCode()).getStatus());

        var invoice = invoices.generateInvoice(new GenerateInvoiceCommand(order.getOrderCode()));
        assertNotNull(invoice.getInvoiceNumber());
        assertTrue(invoice.isSentByEmail());
        assertTrue(invoices.exportInvoice(invoice.getInvoiceNumber()).startsWith("data:application/pdf;base64,"));
        assertEquals(47.69, invoices.calculateTaxes(invoice.getInvoiceNumber()), 0.001);

        carts.addProduct(customer.getPersonId(), product.getProductCode(), 1);
        Order unpaidOrder = checkout.checkout(customer.getPersonId());
        assertThrows(IllegalStateException.class,
                () -> invoices.generateInvoice(new GenerateInvoiceCommand(unpaidOrder.getOrderCode())));
        assertThrows(IllegalStateException.class,
                () -> shipments.createShipment(new CreateShipmentCommand(
                        unpaidOrder.getOrderCode(), warehouse.getWarehouseCode())));
        assertEquals(3, inventory.getAvailableStock(product.getProductCode(), warehouse.getWarehouseCode()));

        Product digitalProduct = products.createProduct(new CreateProductCommand(
                "Digital " + suffix, "Descarga de prueba", 12.0, ProductType.DIGITAL,
                seller.getSellerCode()));
        products.publishProduct(digitalProduct.getProductCode());
        carts.addProduct(customer.getPersonId(), digitalProduct.getProductCode(), 1);
        Order digitalOrder = checkout.checkout(customer.getPersonId());
        assertTrue(orders.processPayment(new ProcessPaymentCommand(digitalOrder.getOrderCode())));
        assertEquals(OrderStatus.COMPLETED, orders.getOrder(digitalOrder.getOrderCode()).getStatus());
    }
}