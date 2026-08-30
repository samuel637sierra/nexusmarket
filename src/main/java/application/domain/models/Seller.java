package application.domain.models;


import lombok.Getter;  
import lombok.Setter;

@Getter
@Setter

public  class Seller extends Person {
 
    private String sellerCode;
    private String businessName;
    private long taxIdentificationNumber;
    
}