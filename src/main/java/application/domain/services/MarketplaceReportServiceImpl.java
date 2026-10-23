package application.domain.services;

import application.domain.ports.out.ReportGeneratorPort;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceReportServiceImpl implements MarketplaceReportService {
    private final ReportGeneratorPort reports;

    public MarketplaceReportServiceImpl(ReportGeneratorPort reports) {
        this.reports = Objects.requireNonNull(reports, "El generador de reportes es obligatorio");
    }

    @Override
    public byte[] generateSalesReport() {
        return requireReport(reports.generateSalesReport());
    }

    @Override
    public byte[] generateInventoryReport() {
        return requireReport(reports.generateInventoryReport());
    }

    @Override
    public byte[] generateShipmentReport() {
        return requireReport(reports.generateShipmentReport());
    }

    private static byte[] requireReport(byte[] report) {
        if (report == null || report.length == 0) {
            throw new IllegalStateException("El generador devolvió un reporte vacío");
        }
        return report.clone();
    }
}