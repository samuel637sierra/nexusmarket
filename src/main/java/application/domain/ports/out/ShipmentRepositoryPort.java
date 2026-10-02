package application.domain.ports.out;

import application.domain.models.Shipment;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepositoryPort {
    Shipment save(Shipment shipment);
    Optional<Shipment> findByCode(String shipmentCode);
    List<Shipment> findAll();
    void updateStatus(String shipmentCode, String status);
}