package application.domain.ports.in;

import application.domain.cmd.CreateProductCommand;
import application.domain.cmd.SuspendProductCommand;
import application.domain.cmd.UpdateProductCommand;
import application.domain.models.Product;

import java.util.List;

/** Puerto de entrada para administrar el catálogo de productos. */
public interface ProductInputPort {

    Product createProduct(CreateProductCommand command);

    Product updateProduct(UpdateProductCommand command);

    void suspendProduct(SuspendProductCommand command);

    Product findByCode(String productCode);

    List<Product> findAllProducts();

    default List<Product> searchProducts(String text) {
        return findAllProducts().stream()
                .filter(product -> text == null || text.isBlank()
                        || (product.getName() != null && product.getName().toLowerCase().contains(text.toLowerCase()))
                        || (product.getDescription() != null && product.getDescription().toLowerCase().contains(text.toLowerCase())))
                .toList();
    }
}