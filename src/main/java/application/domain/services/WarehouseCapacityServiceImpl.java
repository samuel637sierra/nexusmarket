package application.domain.services;

import application.domain.models.Inventory;
import application.domain.models.Warehouse;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WarehouseCapacityServiceImpl implements WarehouseCapacityService {
    private final WarehouseRepositoryPort warehouses;
    private final InventoryRepositoryPort inventories;

    public WarehouseCapacityServiceImpl(WarehouseRepositoryPort warehouses, InventoryRepositoryPort inventories) {
        this.warehouses = Objects.requireNonNull(warehouses, "El repositorio de bodegas es obligatorio");
        this.inventories = Objects.requireNonNull(inventories, "El repositorio de inventario es obligatorio");
    }

    @Override
    public int getOccupancy(String warehouseCode) {
        Warehouse warehouse = findWarehouse(warehouseCode);
        long occupancy = 0;
        for (Inventory inventory : inventories.findByWarehouse(warehouse)) {
            if (inventory != null) {
                occupancy = Math.addExact(occupancy, inventory.getQuantity());
            }
        }
        return Math.toIntExact(occupancy);
    }

    @Override
    public int getRemainingCapacity(String warehouseCode) {
        Warehouse warehouse = findWarehouse(warehouseCode);
        return Math.max(0, warehouse.getCapacity() - getOccupancy(warehouse.getWarehouseCode()));
    }

    private Warehouse findWarehouse(String warehouseCode) {
        if (warehouseCode == null || warehouseCode.isBlank()) {
            throw new IllegalArgumentException("El código de bodega es obligatorio");
        }
        return warehouses.findByCode(warehouseCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Bodega no encontrada: " + warehouseCode));
    }
}