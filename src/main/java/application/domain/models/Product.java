package application.domain.models;

import application.domain.valueObjects.ProductStatus;
import application.domain.valueObjects.ProductType;

import java.util.Objects;

/**
 * Producto comercializable en el catálogo de NexusMarket.
 */
public class Product {

    private String productCode;
    private String name;
    private String description;
    private double price;
    private ProductType type;
    private ProductStatus status;

    public Product() {
        this.status = ProductStatus.SUSPENDED;
    }

    public Product(String productCode, String name, String description, double price,
                   ProductType type) {
        this(productCode, name, description, price, type, ProductStatus.SUSPENDED);
    }

    public Product(String productCode, String name, String description, double price,
                   ProductType type, ProductStatus status) {
        this.productCode = requireText(productCode, "El código del producto es obligatorio");
        this.name = requireText(name, "El nombre del producto es obligatorio");
        this.description = description;
        this.price = requirePrice(price);
        this.type = Objects.requireNonNull(type, "El tipo del producto es obligatorio");
        this.status = Objects.requireNonNull(status, "El estado del producto es obligatorio");
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = requireText(productCode, "El código del producto es obligatorio");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "El nombre del producto es obligatorio");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = requirePrice(price);
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = Objects.requireNonNull(type, "El tipo del producto es obligatorio");
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = Objects.requireNonNull(status, "El estado del producto es obligatorio");
    }

    public void updatePrice(double newPrice) {
        ensureNotDiscontinued();
        price = requirePrice(newPrice);
    }

    public void updateDescription(String description) {
        ensureNotDiscontinued();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
        this.description = description;
    }

    public void publish() {
        ensureNotDiscontinued();
        validateAttributes();
        status = ProductStatus.PUBLISHED;
    }

    public void suspend() {
        ensureNotDiscontinued();
        status = ProductStatus.SUSPENDED;
    }

    public void discontinue() {
        status = ProductStatus.DISCONTINUED;
    }

    public boolean isPublished() {
        return status == ProductStatus.PUBLISHED;
    }

    public boolean isAvailable() {
        return isPublished();
    }

    public void validateForSale() {
        validateAttributes();
        if (!isPublished()) {
            throw new IllegalStateException("El producto no está publicado");
        }
    }

    public boolean requiresShipping() {
        return type == ProductType.PHYSICAL;
    }

    private void ensureNotDiscontinued() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("No se puede modificar un producto descontinuado");
        }
    }

    private void validateAttributes() {
        requireText(productCode, "El código del producto es obligatorio");
        requireText(name, "El nombre del producto es obligatorio");
        Objects.requireNonNull(type, "El tipo del producto es obligatorio");
        requirePrice(price);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static double requirePrice(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("El precio debe ser un número finito mayor que cero");
        }
        return value;
    }
}