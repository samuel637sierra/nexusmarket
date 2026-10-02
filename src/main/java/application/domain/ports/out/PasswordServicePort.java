package application.domain.ports.out;

public interface PasswordServicePort {
    String hash(String plainPassword);
    boolean matches(String plainPassword, String storedPassword);
}