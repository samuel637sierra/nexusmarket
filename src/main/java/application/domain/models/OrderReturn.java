package application.domain.models;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Devolución reembolsada de un producto de un pedido, asociada al vendedor dueño del producto.
 */
public record OrderReturn(String returnCode, String productCode, String productName, String sellerCode,
                          String warehouseCode, int quantity, double amount, LocalDate returnDate)
        implements Serializable {
}
