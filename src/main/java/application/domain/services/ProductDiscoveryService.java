package application.domain.services;

import application.domain.models.Product;
import application.domain.valueObjects.ProductType;

import java.util.List;

public interface ProductDiscoveryService {
    List<Product> findPublished();
    List<Product> searchPublished(String query);
    List<Product> findPublishedByType(ProductType type);
}