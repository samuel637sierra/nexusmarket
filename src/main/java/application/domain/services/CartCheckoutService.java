package application.domain.services;

import application.domain.models.Order;

public interface CartCheckoutService {
    Order checkout(String customerId);
}