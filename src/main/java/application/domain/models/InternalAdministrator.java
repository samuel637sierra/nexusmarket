package application.domain.models; 
  

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter

public   class  InternalAdministrator extends Person {
    
    private String administratorCode;
    private LocalDate hireDate;
   
}
