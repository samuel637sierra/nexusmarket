package application.domain.ports.out;

import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Warehouse;

import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir inventarios. */
public interface InventoryRepositoryPort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProduct(Product product);

    Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse);

    void updateStock(Product product, int quantity);

    List<Inventory> findAll();

    default List<Inventory> findByWarehouse(Warehouse warehouse) {
        return findAll().stream()
                .filter(inventory -> inventory.getWarehouse() == warehouse)
                .toList();
    }
}