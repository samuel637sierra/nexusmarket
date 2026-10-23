package application.domain.services;

import application.domain.cmd.CreateShipmentCommand;
import application.domain.cmd.DeliverShipmentCommand;
import application.domain.cmd.DispatchShipmentCommand;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.domain.models.Product;
import application.domain.valueObjects.OrderStatus;
import application.domain.cmd.RemoveStockCommand;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.ShipmentTrackingPort;
import application.domain.ports.out.WarehouseRepositoryPort;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepositoryPort shipments;
    private final OrderRepositoryPort orders;
    private final WarehouseRepositoryPort warehouses;
    private final ShipmentTrackingPort tracking;
    private final InventoryService inventory;

    public ShipmentServiceImpl(ShipmentRepositoryPort shipments,
                               OrderRepositoryPort orders,
                               WarehouseRepositoryPort warehouses,
                               ShipmentTrackingPort tracking,
                               InventoryService inventory) {
        this.shipments = Objects.requireNonNull(shipments);
        this.orders = Objects.requireNonNull(orders);
        this.warehouses = Objects.requireNonNull(warehouses);
        this.tracking = tracking;
        this.inventory = Objects.requireNonNull(inventory);
    }

    @Override
    public Shipment createShipment(CreateShipmentCommand command) {
        Objects.requireNonNull(command);
        Order order = orders.findByCode(command.orderCode())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (!order.requiresShipping()) {
            throw new IllegalStateException("El pedido no contiene productos físicos que requieran envío");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("El pedido debe estar pagado antes de crear el envío");
        }
        Warehouse warehouse = command.warehouseCode() == null
                ? null
                : warehouses.findByCode(command.warehouseCode())
                .orElseThrow(() -> new IllegalArgumentException("Bodega no encontrada"));
        if (warehouse == null) {
            throw new IllegalArgumentException("La bodega de origen es obligatoria");
        }
        validateStock(order, warehouse);
        String destination = order.getCustomer() == null
                ? null
                : order.getCustomer().getMainAddress();
        Shipment shipment = shipments.save(new Shipment(
                "SHP-" + System.nanoTime(), order, warehouse, destination));
        if (tracking != null) {
            tracking.registerShipment(shipment);
        }
        return shipment;
    }

    @Override
    public void dispatchShipment(DispatchShipmentCommand command) {
        Objects.requireNonNull(command);
        Shipment shipment = find(command.shipmentCode());
        if (Shipment.PREPARING.equals(shipment.getStatus())) {
            if (shipment.getOrder().getStatus() != OrderStatus.PAID) {
                throw new IllegalStateException("El pedido debe estar pagado antes de despacharse");
            }
            validateStock(shipment.getOrder(), shipment.getOriginWarehouse());
            shipment.getOrder().getProducts().forEach((product, quantity) -> {
                if (product.requiresShipping()) {
                    inventory.removeStock(new RemoveStockCommand(product.getProductCode(),
                            shipment.getOriginWarehouse().getWarehouseCode(), quantity));
                }
            });
        }
        shipment.startShipment();
        orders.save(shipment.getOrder());
        shipments.save(shipment);
        if (tracking != null) {
            tracking.registerShipment(shipment);
        }
    }

    @Override
    public void deliverShipment(DeliverShipmentCommand command) {
        Objects.requireNonNull(command);
        Shipment shipment = find(command.shipmentCode());
        shipment.markAsDelivered();
        orders.save(shipment.getOrder());
        shipments.save(shipment);
        if (tracking != null) {
            tracking.confirmDelivery(shipment.getShipmentCode());
        }
    }

    @Override
    public String trackShipment(String code) {
        return find(code).trackShipment();
    }

    private Shipment find(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de envío es obligatorio");
        }
        return shipments.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado: " + code));
    }

    private void validateStock(Order order, Warehouse warehouse) {
        for (var item : order.getProducts().entrySet()) {
            Product product = item.getKey();
            if (!product.requiresShipping()) {
                continue;
            }
            boolean hasStock = inventory.findStock(product.getProductCode(), warehouse.getWarehouseCode())
                    .map(stock -> stock.hasStock(item.getValue()))
                    .orElse(false);
            if (!hasStock) {
                throw new IllegalStateException("Stock insuficiente en la bodega para el producto "
                        + product.getProductCode());
            }
        }
    }
}