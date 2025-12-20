package com.checker;


import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AccessChecker {

    public void checkAdminAccess(Set<String> roles) {

        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new SecurityException("Admin role required");
        }
    }

    public void checkUserAccess(Long targetUserId, Long requesterId, Set<String> roles) {

        if (roles == null) {
            throw new SecurityException("Roles are missing");
        }

        if (roles.contains("ROLE_ADMIN")) return;

        if (roles.contains("ROLE_USER") && targetUserId.equals(requesterId)) return;

        throw new SecurityException("Access denied");
    }
}