package application.domain.ports.out;

public interface ReportGeneratorPort {
    byte[] generateSalesReport();
    byte[] generateInventoryReport();
    byte[] generateShipmentReport();
}