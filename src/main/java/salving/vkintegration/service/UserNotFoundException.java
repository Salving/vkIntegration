package salving.vkintegration.service;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userId) {
        super("User not found: %s".formatted(userId));
    }
}
