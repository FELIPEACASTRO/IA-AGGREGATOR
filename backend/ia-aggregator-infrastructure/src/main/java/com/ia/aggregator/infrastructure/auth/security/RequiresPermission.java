package com.ia.aggregator.infrastructure.auth.security;

import com.ia.aggregator.domain.auth.vo.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce RBAC permissions on controller methods.
 *
 * <p>Usage: {@code @RequiresPermission(Permission.CHAT_CREATE)}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    Permission value();
}
