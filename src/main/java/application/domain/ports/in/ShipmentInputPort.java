package application.domain.ports.in;

import application.domain.cmd.CreateShipmentCommand;
import application.domain.cmd.DeliverShipmentCommand;
import application.domain.cmd.DispatchShipmentCommand;
import application.domain.models.Shipment;

/** Puerto de entrada para la gestión de envíos. */
public interface ShipmentInputPort {

    Shipment createShipment(CreateShipmentCommand command);

    void dispatchShipment(DispatchShipmentCommand command);

    void deliverShipment(DeliverShipmentCommand command);

    String trackShipment(String shipmentCode);
}