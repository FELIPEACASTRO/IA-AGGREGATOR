package com.ia.aggregator.infrastructure.auth.security;

import com.ia.aggregator.application.auth.port.in.PermissionCheckUseCase;
import com.ia.aggregator.application.auth.port.out.AuditPort;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.audit.AuditEvent;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces @RequiresPermission annotations.
 *
 * <p>Checks the authenticated user's permissions against the required permission.
 * Records denied access in the audit trail.
 */
@Aspect
@Component
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    private final PermissionCheckUseCase permissionCheck;
    private final AuditPort auditPort;

    public PermissionAspect(PermissionCheckUseCase permissionCheck, AuditPort auditPort) {
        this.permissionCheck = permissionCheck;
        this.auditPort = auditPort;
    }

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                   RequiresPermission requiresPermission) throws Throwable {
        Permission required = requiresPermission.value();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.AUTH_005, "Authentication required");
        }

        if (!permissionCheck.hasPermission(user.getUserId(), required)) {
            log.warn("Permission denied: user={} requires={}", user.getEmail(), required);

            auditPort.record(AuditEvent.denied(
                    user.getUserId(), user.getEmail(), null,
                    "permission.denied", required.getValue(), null
            ));

            throw new BusinessException(ErrorCode.AUTH_005,
                    "Insufficient permissions: " + required.getValue());
        }

        return joinPoint.proceed();
    }
}
