package application.domain.services;

import application.domain.cmd.CreateProductCommand;
import application.domain.cmd.SuspendProductCommand;
import application.domain.cmd.UpdateProductCommand;
import application.domain.models.Product;
import application.domain.ports.in.ProductInputPort;
import java.util.List;

/** Contrato de casos de uso del catálogo de productos. */
public interface ProductService extends ProductInputPort {
    Product createProduct(CreateProductCommand command);
    Product updateProduct(UpdateProductCommand command);
    void suspendProduct(SuspendProductCommand command);
    Product publishProduct(String productCode);
    void deleteProduct(String productCode);
    Product findProduct(String productCode);
    List<Product> findAllProducts();
    List<Product> searchProducts(String text);
}