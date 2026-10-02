package application.domain.cmd;

public record AddStockCommand(
        String productCode,
        String warehouseCode,
        int quantity
) {
}