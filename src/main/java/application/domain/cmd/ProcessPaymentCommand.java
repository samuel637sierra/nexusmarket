package application.domain.cmd;

public record ProcessPaymentCommand(
        String orderCode
) {
}