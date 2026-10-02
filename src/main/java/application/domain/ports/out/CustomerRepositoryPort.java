package application.domain.ports.out;

import application.domain.models.Customer;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir clientes. */
public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(String customerId);

    List<Customer> findAll();

    void delete(String customerId);
}