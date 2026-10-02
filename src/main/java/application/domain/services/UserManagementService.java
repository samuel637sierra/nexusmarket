package application.domain.services;

import application.domain.cmd.ActivateUserCommand;
import application.domain.cmd.AuthenticateUserCommand;
import application.domain.cmd.BlockUserCommand;
import application.domain.cmd.ChangePasswordCommand;
import application.domain.cmd.RegisterCustomerCommand;
import application.domain.cmd.RegisterSellerCommand;
import application.domain.cmd.UpdateUserCommand;
import application.domain.models.Customer;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.in.UserManagementInputPort;
import java.util.Optional;

public interface UserManagementService extends UserManagementInputPort {
    Customer registerCustomer(RegisterCustomerCommand command);
    Seller registerSeller(RegisterSellerCommand command);
    Optional<User> authenticateUser(AuthenticateUserCommand command);
    User updateUser(UpdateUserCommand command);
    void changePassword(ChangePasswordCommand command);
    void blockUser(BlockUserCommand command);
    void activateUser(ActivateUserCommand command);
}