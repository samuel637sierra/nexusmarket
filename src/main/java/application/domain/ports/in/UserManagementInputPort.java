package application.domain.ports.in;

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

import java.util.Optional;

/** Puerto de entrada para la administración de usuarios. */
public interface UserManagementInputPort {

    Customer registerCustomer(RegisterCustomerCommand command);

    Seller registerSeller(RegisterSellerCommand command);

    Optional<User> authenticateUser(AuthenticateUserCommand command);

    User updateUser(UpdateUserCommand command);

    void changePassword(ChangePasswordCommand command);

    void blockUser(BlockUserCommand command);

    void activateUser(ActivateUserCommand command);
}