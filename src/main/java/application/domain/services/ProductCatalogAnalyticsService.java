package application.domain.services;

import application.domain.valueObjects.ProductType;

import java.util.Map;

public interface ProductCatalogAnalyticsService {
    Map<ProductType, Long> countPublishedProductsByType();
    Map<ProductType, Double> averagePublishedPriceByType();
}