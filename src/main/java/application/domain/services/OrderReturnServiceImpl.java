package application.domain.services;

import application.domain.cmd.AddStockCommand;
import application.domain.models.Order;
import application.domain.models.OrderReturn;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.PaymentGatewayPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class OrderReturnServiceImpl implements OrderReturnService {
    private final OrderRepositoryPort orders;
    private final SellerRepositoryPort sellers;
    private final ShipmentRepositoryPort shipments;
    private final InventoryService inventory;
    private final PaymentGatewayPort paymentGateway;

    public OrderReturnServiceImpl(OrderRepositoryPort orders, SellerRepositoryPort sellers,
                                  ShipmentRepositoryPort shipments, InventoryService inventory,
                                  PaymentGatewayPort paymentGateway) {
        this.orders = Objects.requireNonNull(orders);
        this.sellers = Objects.requireNonNull(sellers);
        this.shipments = Objects.requireNonNull(shipments);
        this.inventory = Objects.requireNonNull(inventory);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
    }

    @Override
    @Transactional
    public OrderReturn returnProduct(String orderCode, String productCode, int quantity) {
        Order order = orders.findByCode(requireText(orderCode, "El código del pedido es obligatorio"))
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderCode));
        String code = requireText(productCode, "El código del producto es obligatorio");
        // Se usa la copia del producto guardada en el pedido para reembolsar el precio pagado.
        Product product = order.getProducts().keySet().stream()
                .filter(item -> code.equals(item.getProductCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El producto no pertenece al pedido: " + code));
        if (!product.requiresShipping()) {
            throw new IllegalStateException("Los productos digitales no admiten devolución");
        }
        String warehouseCode = shipments.findAll().stream()
                .filter(shipment -> shipment.getOrder() != null
                        && order.getOrderCode().equals(shipment.getOrder().getOrderCode()))
                .map(Shipment::getOriginWarehouse)
                .filter(Objects::nonNull)
                .map(warehouse -> warehouse.getWarehouseCode())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El pedido no tiene un envío asociado"));

        OrderReturn orderReturn = order.registerReturn(product, quantity, findSellerCode(code), warehouseCode);
        if (!paymentGateway.refundPartial(order, orderReturn.amount())) {
            throw new IllegalStateException("La pasarela de pagos rechazó el reembolso");
        }
        inventory.addStock(new AddStockCommand(code, warehouseCode, quantity));
        orders.save(order);
        return orderReturn;
    }

    @Override
    public String findSellerCode(String productCode) {
        return sellers.findAll().stream()
                .filter(seller -> seller.getProducts().stream()
                        .anyMatch(product -> Objects.equals(product.getProductCode(), productCode)))
                .map(seller -> seller.getSellerCode())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró el vendedor del producto " + productCode));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
