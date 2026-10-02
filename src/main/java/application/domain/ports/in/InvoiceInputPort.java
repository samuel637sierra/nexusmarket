package application.domain.ports.in;

import application.domain.cmd.GenerateInvoiceCommand;
import application.domain.models.Invoice;

/** Puerto de entrada para facturación. */
public interface InvoiceInputPort {

    Invoice generateInvoice(GenerateInvoiceCommand command);

    String exportInvoice(String invoiceNumber);

    double calculateTaxes(String invoiceNumber);
}