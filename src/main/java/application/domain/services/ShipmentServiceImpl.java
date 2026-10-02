package application.domain.services;

import application.domain.cmd.CreateShipmentCommand;
import application.domain.cmd.DeliverShipmentCommand;
import application.domain.cmd.DispatchShipmentCommand;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.ShipmentTrackingPort;
import application.domain.ports.out.WarehouseRepositoryPort;

import java.util.Objects;

public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepositoryPort shipments;
    private final OrderRepositoryPort orders;
    private final WarehouseRepositoryPort warehouses;
    private final ShipmentTrackingPort tracking;

    public ShipmentServiceImpl(ShipmentRepositoryPort shipments,
                               OrderRepositoryPort orders,
                               WarehouseRepositoryPort warehouses,
                               ShipmentTrackingPort tracking) {
        this.shipments = Objects.requireNonNull(shipments);
        this.orders = Objects.requireNonNull(orders);
        this.warehouses = Objects.requireNonNull(warehouses);
        this.tracking = tracking;
    }

    @Override
    public Shipment createShipment(CreateShipmentCommand command) {
        Objects.requireNonNull(command);
        Order order = orders.findByCode(command.orderCode())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        Warehouse warehouse = command.warehouseCode() == null
                ? null
                : warehouses.findByCode(command.warehouseCode())
                .orElseThrow(() -> new IllegalArgumentException("Bodega no encontrada"));
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
        shipment.startShipment();
        shipments.save(shipment);
    }

    @Override
    public void deliverShipment(DeliverShipmentCommand command) {
        Objects.requireNonNull(command);
        Shipment shipment = find(command.shipmentCode());
        shipment.markAsDelivered();
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
        return shipments.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado: " + code));
    }
}