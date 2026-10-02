package application.domain.cmd;

public record CreateShipmentCommand(
        String orderCode,
        String warehouseCode
) {
}