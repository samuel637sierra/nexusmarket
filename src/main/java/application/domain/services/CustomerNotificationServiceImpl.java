package application.domain.services;

import application.domain.models.Customer;
import application.domain.ports.out.CustomerRepositoryPort;
import application.domain.ports.out.NotificationPort;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerNotificationServiceImpl implements CustomerNotificationService {
    private final CustomerRepositoryPort customers;
    private final NotificationPort notifications;

    public CustomerNotificationServiceImpl(CustomerRepositoryPort customers, NotificationPort notifications) {
        this.customers = Objects.requireNonNull(customers, "El repositorio de clientes es obligatorio");
        this.notifications = Objects.requireNonNull(notifications, "El servicio de notificaciones es obligatorio");
    }

    @Override
    public void sendEmail(String customerId, String message) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        Customer customer = customers.findById(customerId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + customerId));
        notifications.sendEmail(customer.getEmail(), message.trim());
    }
}