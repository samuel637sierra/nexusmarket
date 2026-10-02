package application.domain.ports.out;

import application.domain.models.Customer;
import application.domain.models.Order;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir pedidos. */
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findByCode(String orderCode);

    List<Order> findByCustomer(Customer customer);

    List<Order> findAll();

    void delete(String orderCode);
}