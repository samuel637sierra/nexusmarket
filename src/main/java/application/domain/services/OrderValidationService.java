package application.domain.services;

public interface OrderValidationService {
    void validateForCheckout(String orderCode);
}