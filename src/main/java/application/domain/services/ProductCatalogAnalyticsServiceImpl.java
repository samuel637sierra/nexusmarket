package application.domain.services;

import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valueObjects.ProductType;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalogAnalyticsServiceImpl implements ProductCatalogAnalyticsService {
    private final ProductRepositoryPort products;

    public ProductCatalogAnalyticsServiceImpl(ProductRepositoryPort products) {
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
    }

    @Override
    public Map<ProductType, Long> countPublishedProductsByType() {
        return products.findPublishedProducts().stream()
                .collect(Collectors.groupingBy(product -> product.getType(), Collectors.counting()));
    }

    @Override
    public Map<ProductType, Double> averagePublishedPriceByType() {
        return products.findPublishedProducts().stream()
                .collect(Collectors.groupingBy(product -> product.getType(),
                        Collectors.averagingDouble(product -> product.getPrice())));
    }
}