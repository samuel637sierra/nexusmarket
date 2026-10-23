package application.domain.services;

import java.util.Map;

public interface SellerSalesAnalyticsService {
    double calculateGrossSales(String sellerCode);
    Map<String, Double> calculateSalesByProduct(String sellerCode);
}