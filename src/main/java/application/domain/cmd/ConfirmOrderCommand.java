package application.domain.cmd;

public record ConfirmOrderCommand(
        String orderCode
) {
}