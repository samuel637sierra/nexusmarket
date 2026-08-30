package application.domain.models;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
 
public class Customer extends Person {
    
    private String mainAddress;
    
}