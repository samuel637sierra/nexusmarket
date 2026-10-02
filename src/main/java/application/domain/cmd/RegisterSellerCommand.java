package application.domain.cmd;

public record RegisterSellerCommand(
        String fullName,
        String email,
        String phoneNumber,
        String businessName,
        Long taxIdentificationNumber,
        String username,
        String password,
        String address
) {
    public RegisterSellerCommand(String fullName,
                                 String email,
                                 String phoneNumber,
                                 String businessName,
                                 Long taxIdentificationNumber,
                                 String username,
                                 String password) {
        this(fullName, email, phoneNumber, businessName, taxIdentificationNumber,
                username, password, null);
    }
}