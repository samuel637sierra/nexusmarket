package application.domain.services;

import application.domain.cmd.GenerateInvoiceCommand;
import application.domain.models.Invoice;
import application.domain.ports.in.InvoiceInputPort;

/** Contrato de los casos de uso de facturación. */
public interface InvoiceService extends InvoiceInputPort {
    @Override
    Invoice generateInvoice(GenerateInvoiceCommand command);

    @Override
    double calculateTaxes(String invoiceNumber);

    @Override
    String exportInvoice(String invoiceNumber);
}