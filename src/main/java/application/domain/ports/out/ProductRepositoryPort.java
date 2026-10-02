package application.domain.ports.out;

import application.domain.models.Product;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir productos. */
public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findByCode(String productCode);

    List<Product> findAll();

    List<Product> findPublishedProducts();

    void delete(String productCode);
}