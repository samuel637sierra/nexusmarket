package application.domain.services;

public interface CustomerNotificationService {
    void sendEmail(String customerId, String message);
}