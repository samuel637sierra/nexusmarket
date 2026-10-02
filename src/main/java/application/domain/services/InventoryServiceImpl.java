package application.domain.services;

import application.domain.cmd.AddStockCommand;
import application.domain.cmd.RemoveStockCommand;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Warehouse;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso para controlar las existencias por producto y bodega.
 */
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepositoryPort inventories;
    private final ProductRepositoryPort products;
    private final WarehouseRepositoryPort warehouses;

    public InventoryServiceImpl(InventoryRepositoryPort inventories,
                                ProductRepositoryPort products,
                                WarehouseRepositoryPort warehouses) {
        this.inventories = Objects.requireNonNull(inventories, "El repositorio de inventario es obligatorio");
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
        this.warehouses = Objects.requireNonNull(warehouses, "El repositorio de bodegas es obligatorio");
    }

    @Override
    public void addStock(AddStockCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Product product = findProduct(command.productCode());
        Warehouse warehouse = findWarehouse(command.warehouseCode());
        Inventory inventory = inventories.findByProductAndWarehouse(product, warehouse)
                .orElse(null);

        int currentOccupancy = calculateWarehouseOccupancy(warehouse);
        if (!warehouse.hasCapacityFor(currentOccupancy, command.quantity())) {
            throw new IllegalStateException("La bodega no tiene capacidad disponible");
        }

        if (inventory == null) {
            inventory = new Inventory(newInventoryCode(), product, warehouse, 0);
        }
        inventory.addStock(command.quantity());
        inventories.save(inventory);
    }

    @Override
    public void removeStock(RemoveStockCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        Product product = findProduct(command.productCode());
        Warehouse warehouse = findWarehouse(command.warehouseCode());
        Inventory inventory = inventories.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new IllegalStateException("El producto no está inventariado en la bodega"));
        inventory.removeStock(command.quantity());
        inventories.save(inventory);
    }

    @Override
    public boolean validateStock(Product product, int quantity) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad consultada debe ser mayor que cero");
        }
        return inventories.findAll().stream()
                .filter(inventory -> inventory.getProduct() != null
                        && inventory.getProduct().getProductCode() != null
                        && inventory.getProduct().getProductCode().equals(product.getProductCode()))
                .anyMatch(inventory -> inventory.getQuantity() >= quantity);
    }

    @Override
    public int getAvailableStock(String productCode, String warehouseCode) {
        if (isBlank(productCode) || isBlank(warehouseCode)) {
            return 0;
        }
        return findStock(productCode, warehouseCode)
            .map(inventory -> inventory.getQuantity())
                .orElse(0);
    }

    @Override
    public Optional<Inventory> findStock(String productCode, String warehouseCode) {
        if (isBlank(productCode) || isBlank(warehouseCode)) {
            return Optional.empty();
        }
        Product product = products.findByCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        Warehouse warehouse = warehouses.findByCode(warehouseCode)
                .orElseThrow(() -> new IllegalArgumentException("Bodega no encontrada: " + warehouseCode));
        return inventories.findByProductAndWarehouse(product, warehouse);
    }

    @Override
    public synchronized void transferStock(Product product, int quantity, Warehouse from, Warehouse to) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        Objects.requireNonNull(from, "La bodega de origen es obligatoria");
        Objects.requireNonNull(to, "La bodega de destino es obligatoria");
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad a transferir debe ser mayor que cero");
        }
        if (sameWarehouse(from, to)) {
            throw new IllegalArgumentException("Las bodegas de origen y destino deben ser diferentes");
        }

        Inventory source = inventories.findByProductAndWarehouse(product, from)
                .orElseThrow(() -> new IllegalStateException("El producto no está inventariado en la bodega de origen"));
        source.validateStock(quantity);

        Inventory destination = inventories.findByProductAndWarehouse(product, to)
                .orElse(null);
        if (!to.hasCapacityFor(calculateWarehouseOccupancy(to), quantity)) {
            throw new IllegalStateException("La bodega de destino no tiene capacidad disponible");
        }
        if (destination == null) {
            destination = new Inventory(newInventoryCode(), product, to, 0);
        }

        destination.addStock(quantity);
        try {
            inventories.save(destination);
            source.removeStock(quantity);
            inventories.save(source);
        } catch (RuntimeException exception) {
            destination.removeStock(quantity);
            throw exception;
        }
    }

    private Product findProduct(String productCode) {
        if (isBlank(productCode)) {
            throw new IllegalArgumentException("El código del producto es obligatorio");
        }
        Product product = products.findByCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        if (!product.requiresShipping()) {
            throw new IllegalArgumentException("Los productos digitales no requieren inventario físico");
        }
        return product;
    }

    private Warehouse findWarehouse(String warehouseCode) {
        if (isBlank(warehouseCode)) {
            throw new IllegalArgumentException("El código de bodega es obligatorio");
        }
        return warehouses.findByCode(warehouseCode)
                .orElseThrow(() -> new IllegalArgumentException("Bodega no encontrada: " + warehouseCode));
    }

    private int calculateWarehouseOccupancy(Warehouse warehouse) {
        long occupancy = inventories.findAll().stream()
                .filter(inventory -> inventory.getWarehouse() != null)
                .filter(inventory -> sameWarehouse(inventory.getWarehouse(), warehouse))
            .mapToLong(inventory -> inventory.getQuantity())
                .sum();
        if (occupancy > Integer.MAX_VALUE) {
            throw new ArithmeticException("La ocupación de la bodega excede el límite permitido");
        }
        return (int) occupancy;
    }

    private static boolean sameWarehouse(Warehouse first, Warehouse second) {
        if (first == second) {
            return true;
        }
        return first != null && second != null
                && first.getWarehouseCode() != null
                && first.getWarehouseCode().equals(second.getWarehouseCode());
    }

    private static String newInventoryCode() {
        return "INV-" + UUID.randomUUID();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}