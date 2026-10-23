package application.domain.services;

import application.domain.models.Product;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valueObjects.ProductType;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProductDiscoveryServiceImpl implements ProductDiscoveryService {
    private final ProductRepositoryPort products;

    public ProductDiscoveryServiceImpl(ProductRepositoryPort products) {
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
    }

    @Override
    public List<Product> findPublished() {
        return List.copyOf(products.findPublishedProducts());
    }

    @Override
    public List<Product> searchPublished(String query) {
        if (query == null || query.isBlank()) {
            return findPublished();
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return products.findPublishedProducts().stream()
                .filter(product -> contains(product.getName(), normalized)
                        || contains(product.getDescription(), normalized))
                .toList();
    }

    @Override
    public List<Product> findPublishedByType(ProductType type) {
        Objects.requireNonNull(type, "El tipo de producto es obligatorio");
        return products.findPublishedProducts().stream()
                .filter(product -> product.getType() == type)
                .toList();
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}