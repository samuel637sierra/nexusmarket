package application.domain.models;

import application.domain.valueObjects.SystemRole;
import application.domain.valueObjects.UserStatus;

/**
 * Base de los usuarios autenticados de NexusMarket.
 */
public abstract class User extends Person {

    private long userId;
    private String username;
    private String password;
    private SystemRole role;
    private UserStatus status;

    protected User() {
        this.status = UserStatus.ACTIVE;
    }

    protected User(long userId, String username, String password, SystemRole role, UserStatus status) {
        setUserId(userId);
        setUsername(username);
        setPassword(password);
        setRole(role);
        this.status = status == null ? UserStatus.ACTIVE : status;
    }

    protected User(long userId, String username, String password, SystemRole role,
                   UserStatus status, String personId, String fullName, String email,
                   String phoneNumber, String address, java.time.LocalDate birthDate) {
        super(personId, fullName, email, phoneNumber, address, birthDate);
        setUserId(userId);
        setUsername(username);
        setPassword(password);
        setRole(role);
        this.status = status == null ? UserStatus.ACTIVE : status;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("El identificador del usuario es obligatorio");
        }
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = requireText(username, "El nombre de usuario es obligatorio");
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = requireText(password, "La contraseña es obligatoria");
    }

    public SystemRole getRole() {
        return role;
    }

    public void setRole(SystemRole role) {
        this.role = java.util.Objects.requireNonNull(role, "El rol del usuario es obligatorio");
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = java.util.Objects.requireNonNull(status, "El estado del usuario es obligatorio");
    }

    public void activateAccount() {
        status = UserStatus.ACTIVE;
    }

    public void blockAccount() {
        status = UserStatus.BLOCKED;
    }

    public void deactivateAccount() {
        status = UserStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean hasAccess(SystemRole role) {
        return isActive() && role != null
                && (this.role == role || this.role.isAdministrator());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
