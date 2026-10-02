package application.domain.models;

import application.domain.valueObjects.SystemRole;
import application.domain.valueObjects.UserStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Operador encargado de la preparación y despacho de envíos.
 */
public class LogisticsProvider extends User {

    private String operatorCode;
    private String assignedWarehouse;
    private final List<Shipment> assignedShipments = new ArrayList<>();
    private static final AtomicLong SHIPMENT_SEQUENCE = new AtomicLong();

    public LogisticsProvider() {
        setRole(SystemRole.LOGISTICS_PROVIDER);
        setStatus(UserStatus.ACTIVE);
    }

    public LogisticsProvider(long userId, String username, String password,
                              String operatorCode, String assignedWarehouse,
                              String personId, String fullName, String email,
                              String phoneNumber, String address, LocalDate birthDate) {
        super(userId, username, password, SystemRole.LOGISTICS_PROVIDER, UserStatus.ACTIVE,
                personId, fullName, email, phoneNumber, address, birthDate);
        this.operatorCode = operatorCode;
        this.assignedWarehouse = assignedWarehouse;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getAssignedWarehouse() {
        return assignedWarehouse;
    }

    public void setAssignedWarehouse(String assignedWarehouse) {
        this.assignedWarehouse = assignedWarehouse;
    }

    public Shipment prepareShipment(Order order) {
        throw new IllegalStateException("Se debe especificar la bodega de origen del envío");
    }

    public Shipment prepareShipment(Order order, Warehouse originWarehouse) {
        Objects.requireNonNull(order, "El pedido es obligatorio");
        Objects.requireNonNull(originWarehouse, "La bodega de origen es obligatoria");
        if (!order.requiresShipping()) {
            throw new IllegalStateException("Un pedido sin productos físicos no requiere envío");
        }
        if (order.getStatus() != application.domain.valueObjects.OrderStatus.PAID) {
            throw new IllegalStateException("El pedido debe estar pagado antes de preparar el envío");
        }
        if (assignedWarehouse == null || assignedWarehouse.isBlank()
                || !assignedWarehouse.equals(originWarehouse.getWarehouseCode())) {
            throw new IllegalStateException("La bodega no está asignada al operador logístico");
        }
        String destination = order.getCustomer().getMainAddress();
        Shipment shipment = new Shipment("SHP-" + SHIPMENT_SEQUENCE.incrementAndGet(),
                order, originWarehouse, destination);
        assignedShipments.add(shipment);
        return shipment;
    }

    public void dispatchShipment(Shipment shipment) {
        requireAssignedShipment(shipment);
        shipment.startShipment();
    }

    public void updateShipmentStatus(Shipment shipment, String status) {
        requireAssignedShipment(shipment);
        shipment.updateStatus(status);
    }

    private void requireAssignedShipment(Shipment shipment) {
        Objects.requireNonNull(shipment, "El envío es obligatorio");
        if (!assignedShipments.contains(shipment)) {
            throw new IllegalStateException("El envío no está asignado al operador logístico");
        }
    }

    public List<Shipment> getAssignedShipments() {
        return List.copyOf(assignedShipments);
    }
}