package application.domain.services;

import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueObjects.OrderStatus;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OrderValidationServiceImpl implements OrderValidationService {
    private final OrderRepositoryPort orders;
    private final InventoryRepositoryPort inventories;

    public OrderValidationServiceImpl(OrderRepositoryPort orders, InventoryRepositoryPort inventories) {
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
        this.inventories = Objects.requireNonNull(inventories, "El repositorio de inventario es obligatorio");
    }

    @Override
    public void validateForCheckout(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("El código del pedido es obligatorio");
        }
        Order order = orders.findByCode(orderCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderCode));
        if (order.getStatus() != OrderStatus.CART) {
            throw new IllegalStateException("Solo se pueden validar pedidos en el carrito");
        }
        if (order.isEmpty()) {
            throw new IllegalStateException("No se puede finalizar un pedido vacío");
        }
        if (order.getCustomer() == null || !order.getCustomer().isActive()) {
            throw new IllegalStateException("El pedido debe pertenecer a un cliente activo");
        }

        List<Inventory> allInventories = inventories.findAll();
        order.getProducts().forEach((product, quantity) -> {
            product.validateForSale();
            if (product.requiresShipping()) {
                long available = 0;
                for (Inventory inventory : allInventories) {
                    if (inventory != null && sameProduct(inventory, product)) {
                        available = Math.addExact(available, inventory.getQuantity());
                    }
                }
                if (available < quantity) {
                    throw new IllegalStateException("Stock insuficiente para el producto "
                            + product.getProductCode());
                }
            }
        });
        order.calculateTotal();
    }

    private static boolean sameProduct(Inventory inventory, Product product) {
        return inventory.getProduct() != null
                && Objects.equals(inventory.getProduct().getProductCode(), product.getProductCode());
    }
}