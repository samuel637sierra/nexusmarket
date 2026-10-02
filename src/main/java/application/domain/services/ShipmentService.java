package application.domain.services;

import application.domain.cmd.CreateShipmentCommand;
import application.domain.cmd.DeliverShipmentCommand;
import application.domain.cmd.DispatchShipmentCommand;
import application.domain.models.Shipment;

public interface ShipmentService {
    Shipment createShipment(CreateShipmentCommand command);
    void dispatchShipment(DispatchShipmentCommand command);
    void deliverShipment(DeliverShipmentCommand command);
    String trackShipment(String shipmentCode);
}