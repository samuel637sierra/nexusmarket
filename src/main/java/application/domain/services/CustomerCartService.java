package application.domain.services;

import application.domain.models.Customer;
import application.domain.models.Product;

import java.util.Map;

public interface CustomerCartService {
    Map<Product, Integer> viewCart(String customerId);
    void addProduct(String customerId, String productCode, int quantity);
    void removeProduct(String customerId, String productCode);
    void clearCart(String customerId);
    Customer findCustomer(String customerId);
}