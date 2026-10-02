package application.domain.ports.out;

import application.domain.models.Invoice;
import application.domain.models.Order;

public interface InvoiceGeneratorPort {
    Invoice generateInvoice(Order order);
    byte[] exportPdf(Invoice invoice);
}