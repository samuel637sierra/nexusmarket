package application.domain.models;

import java.util.Objects;

/**
 * Existencia de un producto en una bodega concreta.
 */
public class Inventory {

    private String inventoryCode;
    private Product product;
    private Warehouse warehouse;
    private int quantity;

    public Inventory() {
    }

    public Inventory(String inventoryCode, Product product, Warehouse warehouse, int quantity) {
        this.inventoryCode = requireText(inventoryCode, "El código de inventario es obligatorio");
        this.product = Objects.requireNonNull(product, "El producto es obligatorio");
        this.warehouse = Objects.requireNonNull(warehouse, "La bodega es obligatoria");
        if (!product.requiresShipping()) {
            throw new IllegalArgumentException("Los productos digitales no se almacenan en bodegas");
        }
        setQuantity(quantity);
    }

    public String getInventoryCode() {
        return inventoryCode;
    }

    public void setInventoryCode(String inventoryCode) {
        this.inventoryCode = requireText(inventoryCode, "El código de inventario es obligatorio");
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        if (!product.requiresShipping()) {
            throw new IllegalArgumentException("Los productos digitales no se almacenan en bodegas");
        }
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = Objects.requireNonNull(warehouse, "La bodega es obligatoria");
    }

    public int getQuantity() {
        return quantity;
    }

    public synchronized void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        this.quantity = quantity;
    }

    public synchronized void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad a agregar debe ser mayor que cero");
        }
        this.quantity = Math.addExact(this.quantity, quantity);
    }

    public synchronized void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad a retirar debe ser mayor que cero");
        }
        if (quantity > this.quantity) {
            throw new IllegalStateException("No hay stock suficiente");
        }
        this.quantity -= quantity;
    }

    public void reserveStock(int quantity) {
        removeStock(quantity);
    }

    public boolean hasStock(int quantity) {
        return quantity > 0 && this.quantity >= quantity;
    }

    public int getAvailableStock() {
        return quantity;
    }

    public void validateStock(int requestedQuantity) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        Objects.requireNonNull(warehouse, "La bodega es obligatoria");
        if (!hasStock(requestedQuantity)) {
            throw new IllegalStateException("No hay stock suficiente");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}