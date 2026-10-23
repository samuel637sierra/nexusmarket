package application.domain.services;

public interface MarketplaceReportService {
    byte[] generateSalesReport();
    byte[] generateInventoryReport();
    byte[] generateShipmentReport();
}