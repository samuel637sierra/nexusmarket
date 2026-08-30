package application.domain.models;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter

public  class Supervisor extends Person {

    private String supervisorCode;
    private LocalDate assignmentDate;
    
}