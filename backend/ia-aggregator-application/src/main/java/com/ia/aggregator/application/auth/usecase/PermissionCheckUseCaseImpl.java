package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.port.in.PermissionCheckUseCase;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.vo.Permission;
import com.ia.aggregator.domain.auth.vo.RolePermissions;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * RBAC permission check implementation.
 *
 * <p>Resolves user role from database, then checks against the permission matrix.
 */
@Service
public class PermissionCheckUseCaseImpl implements PermissionCheckUseCase {

    private final UserRepository userRepository;

    public PermissionCheckUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasPermission(UUID userId, Permission permission) {
        User user = findUser(userId);
        return RolePermissions.hasPermission(user.getRole(), permission);
    }

    @Override
    public Set<Permission> getPermissions(UUID userId) {
        User user = findUser(userId);
        return RolePermissions.forRole(user.getRole());
    }

    @Override
    public boolean hasAllPermissions(UUID userId, Set<Permission> permissions) {
        Set<Permission> userPerms = getPermissions(userId);
        return userPerms.containsAll(permissions);
    }

    @Override
    public boolean hasAnyPermission(UUID userId, Set<Permission> permissions) {
        Set<Permission> userPerms = getPermissions(userId);
        return permissions.stream().anyMatch(userPerms::contains);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001, "User not found"));
    }
}
