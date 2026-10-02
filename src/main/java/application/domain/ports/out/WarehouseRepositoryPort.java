package application.domain.ports.out;

import application.domain.models.Warehouse;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para persistir bodegas. */
public interface WarehouseRepositoryPort {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findByCode(String warehouseCode);
    List<Warehouse> findAll();
    void delete(String warehouseCode);
}