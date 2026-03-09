package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.domain.auth.vo.Permission;

import java.util.Set;
import java.util.UUID;

/**
 * Use case for checking user permissions (RBAC).
 */
public interface PermissionCheckUseCase {

    /**
     * Check if a user has a specific permission.
     */
    boolean hasPermission(UUID userId, Permission permission);

    /**
     * Get all permissions for a user.
     */
    Set<Permission> getPermissions(UUID userId);

    /**
     * Check if a user has all of the specified permissions.
     */
    boolean hasAllPermissions(UUID userId, Set<Permission> permissions);

    /**
     * Check if a user has any of the specified permissions.
     */
    boolean hasAnyPermission(UUID userId, Set<Permission> permissions);
}
