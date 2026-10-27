package application.domain;

import application.domain.models.Customer;
import application.domain.models.Inventory;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Warehouse;
import application.domain.valueObjects.OrderStatus;
import application.domain.valueObjects.ProductStatus;
import application.domain.valueObjects.ProductType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainRulesTests {

    @Test
    void orderCanBeCancelledBeforeShipmentButNotAfterIt() {
        Order cart = new Order("ORD-CART", customer());
        cart.cancelOrder();
        assertEquals(OrderStatus.CANCELLED, cart.getStatus());

        Order shipped = orderWithPhysicalProduct();
        shipped.confirmOrder();
        shipped.markAsPaid();
        shipped.markAsShipped();
        assertThrows(IllegalStateException.class, shipped::cancelOrder);
        assertEquals(OrderStatus.SHIPPED, shipped.getStatus());
    }

    @Test
    void orderRejectsEmptyConfirmationAndRoundsItsTotalToCents() {
        Order order = new Order("ORD-TOTAL", customer());
        assertThrows(IllegalStateException.class, order::confirmOrder);

        order.addProduct(product("PROD-TOTAL", ProductType.DIGITAL, 19.9975), 2);
        assertEquals(40.00, order.calculateTotal(), 0.001);
        order.confirmOrder();
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    @Test
    void inventoryRejectsInvalidQuantitiesAndPreventsOverselling() {
        Product physical = product("PROD-STOCK", ProductType.PHYSICAL, 10.0);
        Inventory inventory = new Inventory("INV-TEST", physical,
                new Warehouse("WH-TEST", "Central", "Calle 1", 10), 3);

        assertThrows(IllegalArgumentException.class, () -> inventory.addStock(0));
        assertThrows(IllegalArgumentException.class, () -> inventory.removeStock(-1));
        assertThrows(IllegalStateException.class, () -> inventory.removeStock(4));
        inventory.removeStock(2);
        assertEquals(1, inventory.getAvailableStock());
    }

    @Test
    void inventoryRejectsDigitalProducts() {
        Product digital = product("PROD-DIGITAL", ProductType.DIGITAL, 5.0);
        Warehouse warehouse = new Warehouse("WH-DIGITAL", "Central", "Calle 1", 10);

        assertThrows(IllegalArgumentException.class,
                () -> new Inventory("INV-DIGITAL", digital, warehouse, 1));
        assertThrows(IllegalArgumentException.class, () -> warehouse.receiveProduct(digital, 1));
    }

    @Test
    void warehouseEnforcesItsCapacity() {
        Warehouse warehouse = new Warehouse("WH-CAP", "Central", "Calle 1", 5);
        Product physical = product("PROD-CAP", ProductType.PHYSICAL, 7.5);

        warehouse.receiveProduct(physical, 4);
        assertEquals(4, warehouse.calculateOccupancy());
        assertFalse(warehouse.hasCapacity(2));
        assertThrows(IllegalStateException.class, () -> warehouse.receiveProduct(physical, 2));
    }

    @Test
    void invoiceUsesOrderTotalAndRejectsNegativeAmounts() {
        Order order = new Order("ORD-INVOICE", customer());
        order.addProduct(product("PROD-INVOICE", ProductType.DIGITAL, 12.5), 3);
        Invoice invoice = new Invoice(order);

        assertEquals(37.5, invoice.getTotal(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> invoice.setTotal(-1));
        invoice.sendInvoiceByEmail();
        assertTrue(invoice.isSentByEmail());
        assertTrue(invoice.getInvoiceNumber().startsWith("INV-"));
    }

    @Test
    void discontinuedProductCannotBePublishedOrEdited() {
        Product product = product("PROD-DISC", ProductType.PHYSICAL, 8.0);
        product.discontinue();

        assertThrows(IllegalStateException.class, product::publish);
        assertThrows(IllegalStateException.class, () -> product.updatePrice(9.0));
        assertEquals(ProductStatus.DISCONTINUED, product.getStatus());
    }

    private static Order orderWithPhysicalProduct() {
        Order order = new Order("ORD-SHIPPED", customer());
        order.addProduct(product("PROD-SHIPPED", ProductType.PHYSICAL, 10.0), 1);
        return order;
    }

    private static Customer customer() {
        return new Customer(1L, "customer", "hashed-password", "PERSON-1", "Test Customer",
                "customer@example.com", "3001234567", "Calle 1", LocalDate.of(1990, 1, 1),
                "Calle 1");
    }

    private static Product product(String code, ProductType type, double price) {
        return new Product(code, "Test product", "Description", price, type, ProductStatus.PUBLISHED);
    }
}
