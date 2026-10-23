package application.domain.services;

import application.domain.models.Order;

import java.util.List;

public interface OrderHistoryService {
    List<Order> findByCustomer(String customerId);
    List<Order> findAll();
}