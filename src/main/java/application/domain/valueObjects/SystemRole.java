package application.domain.valueObjects;

/**
 * Roles que pueden asumir los usuarios de la plataforma.
 */
public enum SystemRole {
    CUSTOMER,
    SELLER,
    LOGISTICS_PROVIDER,
    ADMINISTRATOR,
    SUPERVISOR;

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }
}