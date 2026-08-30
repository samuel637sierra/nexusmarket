package application.domain.models;



import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public  class LogisticsProvider extends Person {
    
    private String operatorCode;
    private String assignedWarehouse;
    

    
}