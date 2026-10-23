package application.api;

import application.domain.models.Customer;
import application.domain.models.Invoice;
import application.domain.models.LogisticsProvider;
import application.domain.models.Order;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueObjects.SystemRole;
import application.domain.valueObjects.OrderStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class MarketplaceAccessPolicy {
    private final UserRepositoryPort users;
    private final SellerRepositoryPort sellers;
    private final OrderRepositoryPort orders;
    private final ShipmentRepositoryPort shipments;
    private final InvoiceRepositoryPort invoices;

    public MarketplaceAccessPolicy(UserRepositoryPort users, SellerRepositoryPort sellers,
                                   OrderRepositoryPort orders,
                                   ShipmentRepositoryPort shipments, InvoiceRepositoryPort invoices) {
        this.users = Objects.requireNonNull(users);
        this.sellers = Objects.requireNonNull(sellers);
        this.orders = Objects.requireNonNull(orders);
        this.shipments = Objects.requireNonNull(shipments);
        this.invoices = Objects.requireNonNull(invoices);
    }

    public void requireCustomerAccess(String customerId, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        if (actor.getRole() != SystemRole.CUSTOMER || !Objects.equals(actor.getPersonId(), customerId)) {
            throw denied();
        }
    }

    public void requireOrderAccess(String orderCode, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        Order order = orders.findByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderCode));
        if (actor.getRole() != SystemRole.CUSTOMER || order.getCustomer() == null
                || !Objects.equals(actor.getPersonId(), order.getCustomer().getPersonId())) {
            throw denied();
        }
    }

    public void requireSellerAccess(String sellerCode, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        Seller seller = sellers.findBySellerCode(sellerCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado: " + sellerCode));
        if (actor.getRole() != SystemRole.SELLER || actor.getUserId() != seller.getUserId()) {
            throw denied();
        }
    }

    public void requireProductSellerAccess(String productCode, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        if (actor.getRole() != SystemRole.SELLER) {
            throw denied();
        }
        boolean ownsProduct = sellers.findAll().stream()
                .filter(seller -> seller.getUserId() == actor.getUserId())
                .flatMap(seller -> seller.getProducts().stream())
                .anyMatch(product -> Objects.equals(product.getProductCode(), productCode));
        if (!ownsProduct) {
            throw denied();
        }
    }

    public void requireWarehouseAccess(String warehouseCode, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        if (!(actor instanceof LogisticsProvider logistics)
                || !Objects.equals(logistics.getAssignedWarehouse(), warehouseCode)) {
            throw denied();
        }
    }

    public void requireShipmentAccess(String shipmentCode, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        Shipment shipment = shipments.findByCode(shipmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado: " + shipmentCode));
        if (actor instanceof LogisticsProvider logistics
                && Objects.equals(logistics.getAssignedWarehouse(),
                        shipment.getOriginWarehouse().getWarehouseCode())) {
            return;
        }
        if (actor instanceof Customer customer && shipment.getOrder().getCustomer() != null
                && Objects.equals(customer.getPersonId(), shipment.getOrder().getCustomer().getPersonId())) {
            return;
        }
        throw denied();
    }

    public void requireShipmentCreationAccess(String orderCode, String warehouseCode,
                                              Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        if (actor instanceof LogisticsProvider logistics
                && Objects.equals(logistics.getAssignedWarehouse(), warehouseCode)) {
            Order order = orders.findByCode(orderCode)
                    .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderCode));
            if (order.getStatus() == OrderStatus.PAID && order.requiresShipping()) {
                return;
            }
        }
        throw denied();
    }

    public void requireInvoiceAccess(String invoiceNumber, Authentication authentication) {
        User actor = currentUser(authentication);
        if (isOperator(actor)) {
            return;
        }
        Invoice invoice = invoices.findByNumber(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + invoiceNumber));
        Customer owner = invoice.getOrder() == null ? null : invoice.getOrder().getCustomer();
        if (actor.getRole() != SystemRole.CUSTOMER || owner == null
                || !Objects.equals(actor.getPersonId(), owner.getPersonId())) {
            throw denied();
        }
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw denied();
        }
        return users.findByUsername(authentication.getName()).orElseThrow(MarketplaceAccessPolicy::denied);
    }

    private static boolean isOperator(User user) {
        return user.getRole() == SystemRole.ADMINISTRATOR || user.getRole() == SystemRole.SUPERVISOR;
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("No tienes permiso para acceder a este recurso");
    }
}
