package application.domain.cmd;

import application.domain.valueObjects.ProductType;

/** Entrada del caso de uso de creación de productos. */
public record CreateProductCommand(
        String name,
        String description,
        double price,
        ProductType type,
        String sellerCode
) {
}