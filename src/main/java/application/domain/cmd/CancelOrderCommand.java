package application.domain.cmd;

public record CancelOrderCommand(
        String orderCode
) {
}