package application.domain.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bodega donde se almacenan los productos físicos.
 */
public class Warehouse {

    private String warehouseCode;
    private String name;
    private String address;
    private int capacity;
    private final List<Inventory> inventories = new ArrayList<>();
    private static final AtomicLong INVENTORY_SEQUENCE = new AtomicLong();

    public Warehouse() {
    }

    public Warehouse(String warehouseCode, String name, String address, int capacity) {
        this.warehouseCode = requireText(warehouseCode, "El código de bodega es obligatorio");
        this.name = requireText(name, "El nombre de la bodega es obligatorio");
        this.address = requireText(address, "La dirección de la bodega es obligatoria");
        setCapacity(capacity);
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = requireText(warehouseCode, "El código de bodega es obligatorio");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "El nombre de la bodega es obligatorio");
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = requireText(address, "La dirección de la bodega es obligatoria");
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero");
        }
        if (calculateOccupancy() > capacity) {
            throw new IllegalStateException("La nueva capacidad es menor que la ocupación actual");
        }
        this.capacity = capacity;
    }

    public synchronized void receiveProduct(Product product, int quantity) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        if (!product.requiresShipping()) {
            throw new IllegalArgumentException("Los productos digitales no se almacenan en bodegas");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (!hasCapacity(quantity)) {
            throw new IllegalStateException("La bodega no tiene capacidad disponible");
        }
        Inventory inventory = findInventory(product);
        if (inventory == null) {
            String code = "INV-" + INVENTORY_SEQUENCE.incrementAndGet();
            inventory = new Inventory(code, product, this, 0);
            inventories.add(inventory);
        }
        inventory.addStock(quantity);
    }

    public synchronized void dispatchProduct(Product product, int quantity) {
        Objects.requireNonNull(product, "El producto es obligatorio");
        Inventory inventory = findInventory(product);
        if (inventory == null) {
            throw new IllegalStateException("El producto no está almacenado en la bodega");
        }
        inventory.removeStock(quantity);
    }

    public synchronized int calculateOccupancy() {
        long occupancy = inventories.stream()
            .mapToLong(inventory -> inventory.getQuantity())
                .sum();
        if (occupancy > Integer.MAX_VALUE) {
            throw new ArithmeticException("La ocupación de la bodega excede el límite permitido");
        }
        return (int) occupancy;
    }

    public boolean hasCapacity(int quantity) {
        return hasCapacityFor(calculateOccupancy(), quantity);
    }

    public boolean hasCapacityFor(int currentOccupancy, int additionalQuantity) {
        if (currentOccupancy < 0 || additionalQuantity < 0) {
            return false;
        }
        return (long) currentOccupancy + additionalQuantity <= capacity;
    }

    public List<Inventory> getInventories() {
        return Collections.unmodifiableList(List.copyOf(inventories));
    }

    public synchronized Inventory findInventory(Product product) {
        if (product == null) {
            return null;
        }
        return inventories.stream()
                .filter(inventory -> inventory.getProduct() == product
                        || (inventory.getProduct() != null && product.getProductCode() != null
                        && product.getProductCode().equals(inventory.getProduct().getProductCode())))
                .findFirst()
                .orElse(null);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}