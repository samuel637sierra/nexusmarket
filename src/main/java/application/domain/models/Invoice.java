package application.domain.models;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factura asociada a un pedido.
 */
public class Invoice {

    private String invoiceNumber;
    private Order order;
    private LocalDate issueDate;
    private double total;
    private boolean sentByEmail;
    private static final AtomicLong SEQUENCE = new AtomicLong();

    public Invoice() {
    }

    public Invoice(String invoiceNumber, Order order) {
        this.invoiceNumber = invoiceNumber;
        this.order = order;
        this.issueDate = LocalDate.now();
        calculateTotal();
    }

    public Invoice(Order order) {
        this("INV-" + SEQUENCE.incrementAndGet(), order);
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        if (total < 0) {
            throw new IllegalArgumentException("El total de la factura no puede ser negativo");
        }
        this.total = total;
    }

    public boolean isSentByEmail() {
        return sentByEmail;
    }

    public void generateInvoice() {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            invoiceNumber = "INV-" + SEQUENCE.incrementAndGet();
        }
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }
        calculateTotal();
    }

    public void calculateTotal() {
        if (order == null) {
            total = 0.0;
        } else {
            total = order.calculateTotal();
        }
    }

    public String exportPDF() {
        generateInvoice();
        return "PDF|Invoice=" + invoiceNumber
                + "|Order=" + (order == null ? "" : order.getOrderCode())
                + "|Total=" + total;
    }

    public void sendInvoiceByEmail() {
        generateInvoice();
        sentByEmail = true;
    }
}