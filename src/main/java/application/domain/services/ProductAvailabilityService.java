package application.domain.services;

import java.util.Map;

public interface ProductAvailabilityService {
    int getTotalAvailable(String productCode);
    Map<String, Integer> getStockByWarehouse(String productCode);
    boolean hasAvailableStock(String productCode, int quantity);
}