package application.domain.services;

public interface ShipmentAnalyticsService {
    Metrics summarize();

    record Metrics(long total, long preparing, long inTransit, long delivered, double averageDeliveryDays) {
    }
}