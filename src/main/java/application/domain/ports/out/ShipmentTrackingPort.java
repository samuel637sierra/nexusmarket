package application.domain.ports.out;

import application.domain.models.Shipment;

public interface ShipmentTrackingPort {
    void registerShipment(Shipment shipment);
    String getShipmentStatus(String shipmentCode);
    void confirmDelivery(String shipmentCode);
}