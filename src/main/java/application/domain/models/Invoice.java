package application.domain.models;


import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class Invoice {
    private String invoiceNumber;
    private Order order;
    private LocalDate issueDate;
    private double total;
}
