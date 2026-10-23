package application.domain.services;

import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProductAvailabilityServiceImpl implements ProductAvailabilityService {
    private final InventoryRepositoryPort inventories;
    private final ProductRepositoryPort products;

    public ProductAvailabilityServiceImpl(InventoryRepositoryPort inventories, ProductRepositoryPort products) {
        this.inventories = Objects.requireNonNull(inventories, "El repositorio de inventario es obligatorio");
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
    }

    @Override
    public int getTotalAvailable(String productCode) {
        Product product = findProduct(productCode);
        long total = 0;
        for (Inventory inventory : inventories.findAll()) {
            if (inventory != null && sameProduct(inventory, product)) {
                total = Math.addExact(total, inventory.getQuantity());
            }
        }
        return Math.toIntExact(total);
    }

    @Override
    public Map<String, Integer> getStockByWarehouse(String productCode) {
        Product product = findProduct(productCode);
        Map<String, Integer> quantities = new HashMap<>();
        List<Inventory> allInventories = inventories.findAll();
        for (Inventory inventory : allInventories) {
            if (inventory == null || !sameProduct(inventory, product) || inventory.getWarehouse() == null) {
                continue;
            }
            String warehouseCode = inventory.getWarehouse().getWarehouseCode();
            Integer existingQuantity = quantities.get(warehouseCode);
            int updatedQuantity = existingQuantity == null
                    ? inventory.getQuantity()
                    : Math.addExact(existingQuantity.intValue(), inventory.getQuantity());
            quantities.put(warehouseCode, updatedQuantity);
        }
        return Map.copyOf(quantities);
    }

    @Override
    public boolean hasAvailableStock(String productCode, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        return getTotalAvailable(productCode) >= quantity;
    }

    private Product findProduct(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("El código del producto es obligatorio");
        }
        return products.findByCode(productCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
    }

    private static boolean sameProduct(Inventory inventory, Product product) {
        return inventory.getProduct() != null
                && Objects.equals(inventory.getProduct().getProductCode(), product.getProductCode());
    }
}