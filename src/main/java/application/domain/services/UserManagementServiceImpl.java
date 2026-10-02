package application.domain.services;

import application.domain.cmd.ActivateUserCommand;
import application.domain.cmd.AuthenticateUserCommand;
import application.domain.cmd.BlockUserCommand;
import application.domain.cmd.ChangePasswordCommand;
import application.domain.cmd.RegisterCustomerCommand;
import application.domain.cmd.RegisterSellerCommand;
import application.domain.cmd.UpdateUserCommand;
import application.domain.models.Customer;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de registro, autenticación y administración de usuarios.
 */
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepositoryPort users;
    private final CustomerRepositoryPort customers;
    private final SellerRepositoryPort sellers;
    private final PasswordServicePort passwords;

    public UserManagementServiceImpl(UserRepositoryPort users,
                                     CustomerRepositoryPort customers,
                                     SellerRepositoryPort sellers,
                                     PasswordServicePort passwords) {
        this.users = Objects.requireNonNull(users, "El repositorio de usuarios es obligatorio");
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.sellers = Objects.requireNonNull(sellers, "El repositorio de vendedores es obligatorio");
        this.passwords = Objects.requireNonNull(passwords, "El servicio de contraseñas es obligatorio");
    }

    @Override
    public Customer registerCustomer(RegisterCustomerCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        validateCommonRegistration(command.fullName(), command.email(), command.phoneNumber(),
                command.username(), command.password());
        String address = requireText(command.address(), "La dirección es obligatoria");
        ensureUsernameAvailable(command.username());

        Customer customer = new Customer(
                newUserId(),
                command.username().trim(),
                passwords.hash(command.password()),
                newPersonId(),
                command.fullName().trim(),
                normalizeEmail(command.email()),
                command.phoneNumber().trim(),
                address,
                null,
                address);
        users.save(customer);
        return customers.save(customer);
    }

    @Override
    public Seller registerSeller(RegisterSellerCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        validateCommonRegistration(command.fullName(), command.email(), command.phoneNumber(),
                command.username(), command.password());
        if (command.businessName() == null || command.businessName().isBlank()) {
            throw new IllegalArgumentException("El nombre comercial es obligatorio");
        }
        if (command.taxIdentificationNumber() == null || command.taxIdentificationNumber() <= 0) {
            throw new IllegalArgumentException("El número de identificación tributaria es obligatorio");
        }
        String address = requireText(command.address(), "La dirección es obligatoria");
        ensureUsernameAvailable(command.username());

        Seller seller = new Seller(
                newUserId(),
                command.username().trim(),
                passwords.hash(command.password()),
                "SEL-" + UUID.randomUUID(),
                command.businessName().trim(),
                command.taxIdentificationNumber(),
                newPersonId(),
                command.fullName().trim(),
                normalizeEmail(command.email()),
                command.phoneNumber().trim(),
                address,
                null);
        users.save(seller);
        return sellers.save(seller);
    }

    @Override
    public Optional<User> authenticateUser(AuthenticateUserCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        String username = requireText(command.username(), "El nombre de usuario es obligatorio");
        String password = requireText(command.password(), "La contraseña es obligatoria");
        return users.findByUsername(username)
            .filter(user -> user.isActive())
                .filter(user -> passwords.matches(password, user.getPassword()));
    }

    @Override
    public User updateUser(UpdateUserCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        User user = findUser(command.userId());
        String fullName = requireText(command.fullName(), "El nombre completo es obligatorio");
        String email = normalizeEmail(requireText(command.email(), "El correo es obligatorio"));
        String phoneNumber = requireText(command.phoneNumber(), "El teléfono es obligatorio");
        String address = requireText(command.address(), "La dirección es obligatoria");

        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber.trim());
        user.updateAddress(address);
        if (user instanceof Customer customer) {
            customer.updateMainAddress(address);
            customers.save(customer);
        }
        return users.save(user);
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        User user = findUser(command.userId());
        String newPassword = requireText(command.newPassword(), "La nueva contraseña es obligatoria");
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos ocho caracteres");
        }
        user.setPassword(passwords.hash(newPassword));
        users.save(user);
    }

    @Override
    public void blockUser(BlockUserCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        User user = findUser(command.userId());
        user.blockAccount();
        users.save(user);
    }

    @Override
    public void activateUser(ActivateUserCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        User user = findUser(command.userId());
        user.activateAccount();
        users.save(user);
    }

    private User findUser(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("El identificador del usuario es obligatorio");
        }
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    private void ensureUsernameAvailable(String username) {
        String normalized = username.trim();
        if (users.existsByUsername(normalized)) {
            throw new IllegalStateException("El nombre de usuario ya está registrado");
        }
    }

    private static void validateCommonRegistration(String fullName, String email, String phoneNumber,
                                                   String username, String password) {
        requireText(fullName, "El nombre completo es obligatorio");
        normalizeEmail(requireText(email, "El correo es obligatorio"));
        requireText(phoneNumber, "El teléfono es obligatorio");
        requireText(username, "El nombre de usuario es obligatorio");
        String plainPassword = requireText(password, "La contraseña es obligatoria");
        if (plainPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos ocho caracteres");
        }
    }

    private static String normalizeEmail(String email) {
        String normalized = requireText(email, "El correo es obligatorio").trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf('@');
        if (separator <= 0 || separator != normalized.lastIndexOf('@')
                || separator == normalized.length() - 1 || normalized.startsWith(".")
                || normalized.endsWith(".") || normalized.contains(" ")) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido");
        }
        return normalized;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String newPersonId() {
        return "PER-" + UUID.randomUUID();
    }

    private static long newUserId() {
        long id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        return id == 0 ? 1 : id;
    }
}