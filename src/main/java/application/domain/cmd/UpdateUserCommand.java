package application.domain.cmd;

public record UpdateUserCommand(
        long userId,
        String fullName,
        String email,
        String phoneNumber,
        String address
) {
}