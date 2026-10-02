package application.domain.cmd;

public record RegisterCustomerCommand(
        String fullName,
        String email,
        String phoneNumber,
        String address,
        String username,
        String password
) {
}