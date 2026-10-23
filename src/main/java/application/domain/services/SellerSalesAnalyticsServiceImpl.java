package application.domain.services;

import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valueObjects.OrderStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SellerSalesAnalyticsServiceImpl implements SellerSalesAnalyticsService {
    private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final SellerRepositoryPort sellers;
    private final OrderRepositoryPort orders;

    public SellerSalesAnalyticsServiceImpl(SellerRepositoryPort sellers, OrderRepositoryPort orders) {
        this.sellers = Objects.requireNonNull(sellers, "El repositorio de vendedores es obligatorio");
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
    }

    @Override
    public double calculateGrossSales(String sellerCode) {
        BigDecimal total = BigDecimal.ZERO;
        for (Double amount : calculateSalesByProduct(sellerCode).values()) {
            if (amount != null) {
                total = total.add(BigDecimal.valueOf(amount.doubleValue()));
            }
        }
        return total.doubleValue();
    }

    @Override
    public Map<String, Double> calculateSalesByProduct(String sellerCode) {
        Seller seller = findSeller(sellerCode);
        Map<String, Product> sellerProducts = new HashMap<>();
        for (Product product : seller.getProducts()) {
            if (product != null && product.getProductCode() != null) {
                sellerProducts.putIfAbsent(product.getProductCode(), product);
            }
        }

        Map<String, BigDecimal> totalsByProduct = new HashMap<>();
        for (Order order : orders.findAll()) {
            if (order == null || !REVENUE_STATUSES.contains(order.getStatus())) {
                continue;
            }
            for (Map.Entry<Product, Integer> item : order.getProducts().entrySet()) {
                Product product = item.getKey();
                Integer quantity = item.getValue();
                if (product == null || quantity == null || !sellerProducts.containsKey(product.getProductCode())) {
                    continue;
                }
                BigDecimal itemTotal = BigDecimal.valueOf(product.getPrice())
                        .multiply(BigDecimal.valueOf(quantity.intValue()));
                BigDecimal currentTotal = totalsByProduct.get(product.getProductCode());
                totalsByProduct.put(product.getProductCode(),
                        currentTotal == null ? itemTotal : currentTotal.add(itemTotal));
            }
        }

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : totalsByProduct.entrySet()) {
            result.put(entry.getKey(), entry.getValue().doubleValue());
        }
        return Map.copyOf(result);
    }

    private Seller findSeller(String sellerCode) {
        if (sellerCode == null || sellerCode.isBlank()) {
            throw new IllegalArgumentException("El código del vendedor es obligatorio");
        }
        return sellers.findBySellerCode(sellerCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado: " + sellerCode));
    }
}