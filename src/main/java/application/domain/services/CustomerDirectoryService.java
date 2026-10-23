package application.domain.services;

import application.domain.models.Customer;

import java.util.List;

public interface CustomerDirectoryService {
    Customer findById(String customerId);
    List<Customer> findAll();
    void delete(String customerId);
}