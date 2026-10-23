package application.domain.services;

import application.domain.valueObjects.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public interface CustomerOrderSummaryService {
    Summary summarize(String customerId);

    record Summary(long totalOrders, BigDecimal lifetimeSpend, Map<OrderStatus, Long> ordersByStatus) {
    }
}