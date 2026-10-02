package application.domain.cmd;

public record RemoveStockCommand(
        String productCode,
        String warehouseCode,
        int quantity
) {
}