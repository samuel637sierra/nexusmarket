package application.domain.models;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class Inventory {
    
    private String inventoryCode;
    private Product product;
    private Warehouse warehouse;
    private int quantity;
}
