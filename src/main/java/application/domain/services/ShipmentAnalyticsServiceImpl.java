package application.domain.services;

import application.domain.models.Shipment;
import application.domain.ports.out.ShipmentRepositoryPort;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ShipmentAnalyticsServiceImpl implements ShipmentAnalyticsService {
    private final ShipmentRepositoryPort shipments;

    public ShipmentAnalyticsServiceImpl(ShipmentRepositoryPort shipments) {
        this.shipments = Objects.requireNonNull(shipments, "El repositorio de envíos es obligatorio");
    }

    @Override
    public Metrics summarize() {
        List<Shipment> all = shipments.findAll();
        long preparing = 0;
        long inTransit = 0;
        long delivered = 0;
        long totalDeliveryDays = 0;
        long timedDeliveries = 0;
        for (Shipment shipment : all) {
            if (shipment == null) {
                continue;
            }
            if (Shipment.PREPARING.equals(shipment.getStatus())) {
                preparing++;
            } else if (Shipment.IN_TRANSIT.equals(shipment.getStatus())) {
                inTransit++;
            } else if (Shipment.DELIVERED.equals(shipment.getStatus())) {
                delivered++;
                if (shipment.getShippingDate() != null && shipment.getDeliveryDate() != null) {
                    totalDeliveryDays = Math.addExact(totalDeliveryDays, shipment.calculateDeliveryTime());
                    timedDeliveries++;
                }
            }
        }
        double averageDays = timedDeliveries == 0 ? 0.0 : (double) totalDeliveryDays / timedDeliveries;
        return new Metrics(all.size(), preparing, inTransit, delivered, averageDays);
    }
}