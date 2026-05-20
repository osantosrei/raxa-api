package com.raxa.security;

import com.raxa.domain.user.User;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static UUID currentUserId(Authentication authentication) {
        return currentUser(authentication).getId();
    }

    public static User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
