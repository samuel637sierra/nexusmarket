package application.domain.cmd;

public record GenerateInvoiceCommand(
        String orderCode
) {
}