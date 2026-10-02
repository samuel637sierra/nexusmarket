package application.domain.services;

import application.domain.cmd.CreateProductCommand;
import application.domain.cmd.SuspendProductCommand;
import application.domain.cmd.UpdateProductCommand;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Casos de uso del catálogo de productos.
 */
public class ProductServiceImpl implements ProductService {

    private final ProductRepositoryPort productRepository;
    private final SellerRepositoryPort sellerRepository;

    public ProductServiceImpl(ProductRepositoryPort productRepository,
                              SellerRepositoryPort sellerRepository) {
        this.productRepository = Objects.requireNonNull(productRepository,
                "El repositorio de productos es obligatorio");
        this.sellerRepository = Objects.requireNonNull(sellerRepository,
                "El repositorio de vendedores es obligatorio");
    }

    @Override
    public Product createProduct(CreateProductCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        String name = requireText(command.name(), "El nombre del producto es obligatorio");
        String sellerCode = requireText(command.sellerCode(), "El código del vendedor es obligatorio");
        if (command.price() <= 0 || !Double.isFinite(command.price())) {
            throw new IllegalArgumentException("El precio debe ser un número finito mayor que cero");
        }
        if (command.type() == null) {
            throw new IllegalArgumentException("El tipo del producto es obligatorio");
        }

        Seller seller = sellerRepository.findBySellerCode(sellerCode)
                .orElseThrow(() -> new IllegalArgumentException("El vendedor no existe"));
        if (!seller.isActive()) {
            throw new IllegalStateException("El vendedor no está activo");
        }

        Product product = productRepository.save(new Product(
                "PROD-" + UUID.randomUUID(), name, command.description(), command.price(), command.type()));
        seller.addProduct(product);
        sellerRepository.save(seller);
        return product;
    }

    @Override
    public Product updateProduct(UpdateProductCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Product product = findProduct(command.productCode());
        String name = requireText(command.name(), "El nombre del producto es obligatorio");
        String description = requireText(command.description(), "La descripción del producto es obligatoria");
        if (command.price() <= 0 || !Double.isFinite(command.price())) {
            throw new IllegalArgumentException("El precio debe ser un número finito mayor que cero");
        }

        // updatePrice ejecuta primero la validación de estado y evita cambios parciales.
        product.updatePrice(command.price());
        product.updateDescription(description);
        product.setName(name);
        return productRepository.save(product);
    }

    @Override
    public void suspendProduct(SuspendProductCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Product product = findProduct(command.productCode());
        product.suspend();
        productRepository.save(product);
    }

    @Override
    public Product publishProduct(String productCode) {
        Product product = findProduct(productCode);
        product.publish();
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(String productCode) {
        Product product = findProduct(productCode);
        product.discontinue();
        productRepository.save(product);
    }

    @Override
    public Product findProduct(String productCode) {
        String code = requireText(productCode, "El código del producto es obligatorio");
        return productRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + code));
    }

    @Override
    public Product findByCode(String productCode) {
        return findProduct(productCode);
    }

    @Override
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> searchProducts(String text) {
        if (text == null || text.isBlank()) {
            return findAllProducts();
        }
        String query = text.trim().toLowerCase(Locale.ROOT);
        return findAllProducts().stream()
                .filter(Objects::nonNull)
                .filter(product -> contains(product.getName(), query)
                        || contains(product.getDescription(), query))
                .toList();
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}