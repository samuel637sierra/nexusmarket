package application.domain.services;

import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SellerDirectoryServiceImpl implements SellerDirectoryService {
    private final SellerRepositoryPort sellers;

    public SellerDirectoryServiceImpl(SellerRepositoryPort sellers) {
        this.sellers = Objects.requireNonNull(sellers, "El repositorio de vendedores es obligatorio");
    }

    @Override
    public Seller findByCode(String sellerCode) {
        String code = requireText(sellerCode, "El código del vendedor es obligatorio");
        return sellers.findBySellerCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado: " + code));
    }

    @Override
    public List<Seller> findAll() {
        return List.copyOf(sellers.findAll());
    }

    @Override
    public List<Seller> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return sellers.findAll().stream()
                .filter(seller -> contains(seller.getBusinessName(), normalized)
                        || contains(seller.getFullName(), normalized)
                        || contains(seller.getEmail(), normalized))
                .toList();
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}