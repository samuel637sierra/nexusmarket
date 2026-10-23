package application.domain.services;

import application.domain.cmd.GenerateInvoiceCommand;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.ports.out.InvoiceGeneratorPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.NotificationPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueObjects.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InvoiceServiceImpl implements InvoiceService {
    private final OrderRepositoryPort orders;
    private final InvoiceRepositoryPort invoices;
    private final InvoiceGeneratorPort generator;
    private final NotificationPort notifications;
    private final double taxRate;

    public InvoiceServiceImpl(OrderRepositoryPort orders, InvoiceRepositoryPort invoices,
                              InvoiceGeneratorPort generator, NotificationPort notifications,
                              @Value("${nexusmarket.tax.rate:0}") double taxRate) {
        this.orders = Objects.requireNonNull(orders, "El repositorio de pedidos es obligatorio");
        this.invoices = Objects.requireNonNull(invoices, "El repositorio de facturas es obligatorio");
        this.generator = Objects.requireNonNull(generator, "El generador de facturas es obligatorio");
        this.notifications = Objects.requireNonNull(notifications, "El servicio de notificaciones es obligatorio");
        if (!Double.isFinite(taxRate) || taxRate < 0 || taxRate > 1) {
            throw new IllegalArgumentException("La tasa tributaria debe estar entre cero y uno");
        }
        this.taxRate = taxRate;
    }

    @Override
    public Invoice generateInvoice(GenerateInvoiceCommand command) {
        Objects.requireNonNull(command, "El comando es obligatorio");
        String orderCode = requireText(command.orderCode(), "El código del pedido es obligatorio");
        Order order = orders.findByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderCode));
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED
            && order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Solo se pueden facturar pedidos pagados");
        }
        Invoice invoice = Objects.requireNonNull(generator.generateInvoice(order),
                "El generador devolvió una factura vacía");
        invoice.generateInvoice();
        invoice.sendInvoiceByEmail();
        Invoice saved = invoices.save(invoice);
        notifications.sendInvoice(saved);
        return saved;
    }

    @Override
    public double calculateTaxes(String invoiceNumber) {
        Invoice invoice = findInvoice(invoiceNumber);
        return BigDecimal.valueOf(invoice.getTotal())
                .multiply(BigDecimal.valueOf(taxRate))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public String exportInvoice(String invoiceNumber) {
        Invoice invoice = findInvoice(invoiceNumber);
        byte[] pdf = generator.exportPdf(invoice);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("No se pudo exportar la factura");
        }
        return "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdf);
    }

    private Invoice findInvoice(String invoiceNumber) {
        String number = requireText(invoiceNumber, "El número de factura es obligatorio");
        return invoices.findByNumber(number)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + number));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}