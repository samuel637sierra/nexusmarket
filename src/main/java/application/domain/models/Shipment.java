package application.domain.models;

import application.domain.valueObjects.OrderStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Envío de un pedido desde una bodega hasta el cliente.
 */
public class Shipment {

    public static final String PREPARING = "PREPARING";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String DELIVERED = "DELIVERED";

    private String shipmentCode;
    private Order order;
    private Warehouse originWarehouse;
    private String destinationAddress;
    private LocalDate shippingDate;
    private LocalDate deliveryDate;
    private String status;

    public Shipment() {
        this.status = PREPARING;
    }

    public Shipment(String shipmentCode, Order order, Warehouse originWarehouse,
                    String destinationAddress) {
        this.shipmentCode = requireText(shipmentCode, "El código de envío es obligatorio");
        this.order = Objects.requireNonNull(order, "El pedido es obligatorio");
        this.originWarehouse = Objects.requireNonNull(originWarehouse, "La bodega de origen es obligatoria");
        this.destinationAddress = requireText(destinationAddress, "La dirección de destino es obligatoria");
        this.status = PREPARING;
    }

    public String getShipmentCode() {
        return shipmentCode;
    }

    public void setShipmentCode(String shipmentCode) {
        this.shipmentCode = requireText(shipmentCode, "El código de envío es obligatorio");
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = Objects.requireNonNull(order, "El pedido es obligatorio");
    }

    public Warehouse getOriginWarehouse() {
        return originWarehouse;
    }

    public void setOriginWarehouse(Warehouse originWarehouse) {
        this.originWarehouse = Objects.requireNonNull(originWarehouse, "La bodega de origen es obligatoria");
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = requireText(destinationAddress, "La dirección de destino es obligatoria");
    }

    public LocalDate getShippingDate() {
        return shippingDate;
    }

    public void setShippingDate(LocalDate shippingDate) {
        this.shippingDate = shippingDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = validateStatus(status);
    }

    public void startShipment() {
        if (status.equals(IN_TRANSIT)) {
            return;
        }
        if (!PREPARING.equals(status)) {
            throw new IllegalStateException("Solo un envío en preparación puede ser despachado");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("El pedido debe estar pagado para despacharlo");
        }
        shippingDate = LocalDate.now();
        status = IN_TRANSIT;
        order.markAsShipped();
    }

    public void dispatchShipment() {
        startShipment();
    }

    public void updateStatus(String status) {
        String nextStatus = validateStatus(status);
        if (this.status.equals(nextStatus)) {
            return;
        }
        switch (nextStatus) {
            case IN_TRANSIT -> startShipment();
            case DELIVERED -> markAsDelivered();
            default -> throw new IllegalStateException("Transición de envío no permitida: "
                    + this.status + " -> " + nextStatus);
        }
    }

    public void markAsDelivered() {
        if (DELIVERED.equals(status)) {
            return;
        }
        if (!IN_TRANSIT.equals(status)) {
            throw new IllegalStateException("Solo un envío en tránsito puede entregarse");
        }
        deliveryDate = LocalDate.now();
        if (deliveryDate.isBefore(shippingDate)) {
            throw new IllegalStateException("La fecha de entrega no puede ser anterior al despacho");
        }
        status = DELIVERED;
        order.markAsDelivered();
    }

    public int calculateDeliveryTime() {
        if (shippingDate == null || deliveryDate == null) {
            throw new IllegalStateException("El envío aún no tiene fechas completas");
        }
        return (int) ChronoUnit.DAYS.between(shippingDate, deliveryDate);
    }

    public String trackShipment() {
        return "Envío{código='" + shipmentCode + "', estado='" + status + "'}";
    }

    public void validate() {
        requireText(shipmentCode, "El código de envío es obligatorio");
        Objects.requireNonNull(order, "El pedido es obligatorio");
        Objects.requireNonNull(originWarehouse, "La bodega de origen es obligatoria");
        requireText(destinationAddress, "La dirección de destino es obligatoria");
        validateStatus(status);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String validateStatus(String status) {
        if (!PREPARING.equals(status) && !IN_TRANSIT.equals(status) && !DELIVERED.equals(status)) {
            throw new IllegalArgumentException("Estado de envío no válido: " + status);
        }
        return status;
    }
}