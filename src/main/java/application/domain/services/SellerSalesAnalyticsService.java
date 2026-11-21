package application.domain.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SellerSalesAnalyticsService {
    double calculateGrossSales(String sellerCode);

    Map<String, Double> calculateSalesByProduct(String sellerCode);

    /** Ventas, inventario y movimientos (compras, devoluciones, reembolsos) de un vendedor. */
    SellerReport buildReport(String sellerCode);

    record SellerReport(String sellerCode, String businessName, int unitsSold, int unitsReturned,
                        double grossSales, double refundedAmount, double netSales,
                        List<ProductLine> products, List<Movement> movements) {
    }

    record ProductLine(String productCode, String name, String type, String status, double price,
                       int unitsSold, int unitsReturned, double grossSales, double refundedAmount,
                       double netSales, int stockAvailable) {
    }

    /** type: SALE (compra) o RETURN (devolución con reembolso). stockChange: efecto en inventario físico. */
    record Movement(LocalDate date, String type, String orderCode, String orderStatus, String productCode,
                    String productName, int quantity, double amount, int stockChange, String reference) {
    }
}
