package application.domain.ports.in;

import application.domain.cmd.AddStockCommand;
import application.domain.cmd.RemoveStockCommand;
import application.domain.models.Inventory;

import java.util.Optional;

/** Puerto de entrada para modificar existencias. */
public interface InventoryInputPort {

    void addStock(AddStockCommand command);

    void removeStock(RemoveStockCommand command);

    int getAvailableStock(String productCode, String warehouseCode);

    Optional<Inventory> findStock(String productCode, String warehouseCode);
}