package application.domain.services;

import application.domain.models.Order;
import application.domain.models.OrderReturn;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valueObjects.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final ProductAvailabilityService availability;
    private final ProductRepositoryPort products;

    public SellerSalesAnalyticsServiceImpl(SellerRepositoryPort sellers, OrderRepositoryPort orders,
                                           ProductAvailabilityService availability,
                                           ProductRepositoryPort products) {
        this.products = Objects.requireNonNull(products, "El repositorio de productos es obligatorio");
        this.availability = Objects.requireNonNull(availability, "El servicio de disponibilidad es obligatorio");
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
        return java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(result));
    }

    @Override
    public SellerReport buildReport(String sellerCode) {
        Seller seller = findSeller(sellerCode);
        Map<String, Product> catalog = new LinkedHashMap<>();
        // La copia guardada en el vendedor puede tener estado o precio desactualizado.
        seller.getProducts().stream().filter(Objects::nonNull)
                .forEach(product -> catalog.putIfAbsent(product.getProductCode(),
                        products.findByCode(product.getProductCode()).orElse(product)));
        Map<String, int[]> units = new HashMap<>();          // [vendidas, devueltas]
        Map<String, BigDecimal[]> amounts = new HashMap<>(); // [bruto, reembolsado]
        List<Movement> movements = new ArrayList<>();
        List<Order> sortedOrders = orders.findAll().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(Order::getOrderDate).thenComparing(Order::getOrderCode)).toList();
        for (Order order : sortedOrders) {
            if (!REVENUE_STATUSES.contains(order.getStatus())) {
                continue;
            }
            boolean dispatched = order.getStatus() != OrderStatus.PAID;
            for (Map.Entry<Product, Integer> item : order.getProducts().entrySet()) {
                Product product = item.getKey();
                if (product == null || !catalog.containsKey(product.getProductCode())) {
                    continue;
                }
                int quantity = item.getValue();
                BigDecimal amount = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(quantity));
                units.computeIfAbsent(product.getProductCode(), key -> new int[2])[0] += quantity;
                amounts.computeIfAbsent(product.getProductCode(), key -> zeros())[0] =
                        amounts.get(product.getProductCode())[0].add(amount);
                int stockChange = product.requiresShipping() && dispatched ? -quantity : 0;
                movements.add(new Movement(order.getOrderDate(), "SALE", order.getOrderCode(),
                        order.getStatus().name(), product.getProductCode(), product.getName(), quantity,
                        money(amount), stockChange, null));
            }
            for (OrderReturn orderReturn : order.getReturns()) {
                if (!catalog.containsKey(orderReturn.productCode())) {
                    continue;
                }
                units.computeIfAbsent(orderReturn.productCode(), key -> new int[2])[1] += orderReturn.quantity();
                amounts.computeIfAbsent(orderReturn.productCode(), key -> zeros())[1] =
                        amounts.get(orderReturn.productCode())[1].add(BigDecimal.valueOf(orderReturn.amount()));
                movements.add(new Movement(orderReturn.returnDate(), "RETURN", order.getOrderCode(),
                        order.getStatus().name(), orderReturn.productCode(), orderReturn.productName(),
                        orderReturn.quantity(), -orderReturn.amount(), orderReturn.quantity(),
                        orderReturn.returnCode()));
            }
        }
        List<ProductLine> lines = new ArrayList<>();
        int sold = 0;
        int returned = 0;
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (Product product : catalog.values()) {
            int[] u = units.getOrDefault(product.getProductCode(), new int[2]);
            BigDecimal[] a = amounts.getOrDefault(product.getProductCode(), zeros());
            int stock = product.requiresShipping() ? availability.getTotalAvailable(product.getProductCode()) : 0;
            lines.add(new ProductLine(product.getProductCode(), product.getName(), product.getType().name(),
                    product.getStatus().name(), product.getPrice(), u[0], u[1], money(a[0]), money(a[1]),
                    money(a[0].subtract(a[1])), stock));
            sold += u[0];
            returned += u[1];
            gross = gross.add(a[0]);
            refunded = refunded.add(a[1]);
        }
        return new SellerReport(seller.getSellerCode(), seller.getBusinessName(), sold, returned,
                money(gross), money(refunded), money(gross.subtract(refunded)), lines, movements);
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private static double money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Seller findSeller(String sellerCode) {
        if (sellerCode == null || sellerCode.isBlank()) {
            throw new IllegalArgumentException("El código del vendedor es obligatorio");
        }
        return sellers.findBySellerCode(sellerCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado: " + sellerCode));
    }
}