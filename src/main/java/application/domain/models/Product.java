package application.domain.models;


import application.domain.valueObjects.ProductType;
import application.domain.valueObjects.ProductStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor

public class Product {

    private String productCode;
    private String name;
    private String description;
    private double price;
    private ProductType type;
    private ProductStatus status;
}
