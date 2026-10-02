package application.domain.services;

import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Warehouse;
import application.domain.ports.in.InventoryInputPort;

import java.util.Optional;

public interface InventoryService extends InventoryInputPort {
    boolean validateStock(Product product, int quantity);

    void transferStock(Product product, int quantity, Warehouse from, Warehouse to);

    @Override
    Optional<Inventory> findStock(String productCode, String warehouseCode);
}
