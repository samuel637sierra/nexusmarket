package application.domain.models;

import application.domain.valueObjects.SystemRole;
import application.domain.valueObjects.UserStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Administrador interno de la plataforma.
 */
public class InternalAdministrator extends User {

    private String administratorCode;
    private LocalDate hireDate;
    private final List<Seller> registeredSellers = new ArrayList<>();
    private final List<Warehouse> assignedWarehouses = new ArrayList<>();
    private final Map<String, String> systemConfiguration = new LinkedHashMap<>();
    private String lastReport;

    public InternalAdministrator() {
        setRole(SystemRole.ADMINISTRATOR);
        setStatus(UserStatus.ACTIVE);
    }

    public InternalAdministrator(long userId, String username, String password,
                                 String administratorCode, LocalDate hireDate,
                                 String personId, String fullName, String email,
                                 String phoneNumber, String address, LocalDate birthDate) {
        super(userId, username, password, SystemRole.ADMINISTRATOR, UserStatus.ACTIVE,
                personId, fullName, email, phoneNumber, address, birthDate);
        this.administratorCode = administratorCode;
        this.hireDate = hireDate;
    }

    public String getAdministratorCode() {
        return administratorCode;
    }

    public void setAdministratorCode(String administratorCode) {
        this.administratorCode = administratorCode;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public void registerSeller(Seller seller) {
        Objects.requireNonNull(seller, "El vendedor es obligatorio");
        if (!registeredSellers.contains(seller)) {
            registeredSellers.add(seller);
        }
    }

    public void assignWarehouse(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "La bodega es obligatoria");
        if (!assignedWarehouses.contains(warehouse)) {
            assignedWarehouses.add(warehouse);
        }
    }

    public void deactivateUser(User user) {
        Objects.requireNonNull(user, "El usuario es obligatorio");
        user.deactivateAccount();
    }

    public void generateReports() {
        lastReport = "Reporte generado el " + LocalDate.now()
                + "; vendedores=" + registeredSellers.size()
                + "; bodegas=" + assignedWarehouses.size();
    }

    public void manageSystemConfiguration() {
        if (systemConfiguration.isEmpty()) {
            systemConfiguration.put("platformName", "NexusMarket");
            systemConfiguration.put("currency", "COP");
        }
    }

    public List<Seller> getRegisteredSellers() {
        return List.copyOf(registeredSellers);
    }

    public List<Warehouse> getAssignedWarehouses() {
        return List.copyOf(assignedWarehouses);
    }

    public Map<String, String> getSystemConfiguration() {
        return Map.copyOf(systemConfiguration);
    }

    public String getLastReport() {
        return lastReport;
    }
}