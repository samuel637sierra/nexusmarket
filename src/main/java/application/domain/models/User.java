package application.domain.models;

import lombok.Getter;
import lombok.Setter; 
import lombok.NoArgsConstructor;

import application.domain.valueObjects.UserStatus;
import application.domain.valueObjects.SystemRole;
@Getter
@Setter 
@NoArgsConstructor

public  abstract class User extends Person {

    private long userId;
    private String username;
    private String password;
    private SystemRole role;
    private UserStatus status;
    
    
}