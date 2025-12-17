package com.checker;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AccessChecker {

    public void checkAdminAccess(Set<String> roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    public void checkUserAccess(Long targetUserId, Long requesterId, Set<String> roles) {
        if (roles == null) {
            throw new AccessDeniedException("Roles are missing");
        }

        if (roles.contains("ROLE_ADMIN")) return;

        if (roles.contains("ROLE_USER") && targetUserId.equals(requesterId)) return;

        throw new AccessDeniedException("Access denied");
    }
}
