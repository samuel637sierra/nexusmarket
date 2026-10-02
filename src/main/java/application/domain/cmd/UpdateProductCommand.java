package application.domain.cmd;

/** Entrada del caso de uso de actualización de productos. */
public record UpdateProductCommand(
        String productCode,
        String name,
        String description,
        double price
) {
}