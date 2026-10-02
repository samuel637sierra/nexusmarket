package application.domain.cmd;

public record ChangePasswordCommand(
        long userId,
        String newPassword
) {
}