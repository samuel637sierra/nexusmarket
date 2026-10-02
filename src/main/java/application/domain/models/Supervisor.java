package application.domain.models;

import application.domain.valueObjects.SystemRole;
import application.domain.valueObjects.UserStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Supervisor de las operaciones del marketplace.
 */
public class Supervisor extends User {

    private String supervisorCode;
    private LocalDate assignmentDate;
    private final List<Order> monitoredOrders = new ArrayList<>();
    private final List<Shipment> reviewedShipments = new ArrayList<>();
    private String monitoringReport;
    private boolean platformVerified;

    public Supervisor() {
        setRole(SystemRole.SUPERVISOR);
        setStatus(UserStatus.ACTIVE);
    }

    public Supervisor(long userId, String username, String password, String supervisorCode,
                      LocalDate assignmentDate, String personId, String fullName,
                      String email, String phoneNumber, String address, LocalDate birthDate) {
        super(userId, username, password, SystemRole.SUPERVISOR, UserStatus.ACTIVE,
                personId, fullName, email, phoneNumber, address, birthDate);
        this.supervisorCode = supervisorCode;
        this.assignmentDate = assignmentDate;
    }

    public String getSupervisorCode() {
        return supervisorCode;
    }

    public void setSupervisorCode(String supervisorCode) {
        this.supervisorCode = supervisorCode;
    }

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }

    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }

    public void monitorOrders() {
        // La colección se mantiene disponible para que el caso de uso pueda
        // registrar los pedidos que le sean asignados.
    }

    public void reviewShipments() {
        // La revisión se representa mediante la colección de envíos revisados.
    }

    public void generateMonitoringReport() {
        monitoringReport = "Reporte de monitoreo: pedidos=" + monitoredOrders.size()
                + ", envíos=" + reviewedShipments.size();
    }

    public void verifyPlatformOperations() {
        platformVerified = true;
    }

    public void addOrderToMonitoring(Order order) {
        if (order != null) {
            monitoredOrders.add(order);
        }
    }

    public void addShipmentToReview(Shipment shipment) {
        if (shipment != null) {
            reviewedShipments.add(shipment);
        }
    }

    public List<Order> getMonitoredOrders() {
        return List.copyOf(monitoredOrders);
    }

    public List<Shipment> getReviewedShipments() {
        return List.copyOf(reviewedShipments);
    }

    public String getMonitoringReport() {
        return monitoringReport;
    }

    public boolean isPlatformVerified() {
        return platformVerified;
    }
}