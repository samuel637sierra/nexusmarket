package application.domain.cmd;

public record AuthenticateUserCommand(
        String username,
        String password
) {
}