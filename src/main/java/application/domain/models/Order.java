package application.domain.models;

import application.domain.valueObjects.OrderStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor

public class Order {
    
   private String orderCode;
   private Customer customer;
   private LocalDate orderDate;
   private double totalAmount;
   private OrderStatus status;

}
