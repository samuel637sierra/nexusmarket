package application.domain.models;


import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public  class Person {

    private String identification;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate birthDate; 
}

