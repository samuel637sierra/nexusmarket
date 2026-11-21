package application.domain.services;

import application.domain.models.OrderReturn;

public interface OrderReturnService {
    /** Devuelve y reembolsa un producto físico de un pedido entregado, reponiendo el stock de su bodega de origen. */
    OrderReturn returnProduct(String orderCode, String productCode, int quantity);

    /** Vendedor dueño del producto, o excepción si ningún vendedor lo publica. */
    String findSellerCode(String productCode);
}
