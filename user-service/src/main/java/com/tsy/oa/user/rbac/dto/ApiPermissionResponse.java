package com.tsy.oa.user.rbac.dto;

import com.tsy.oa.user.rbac.model.ApiPermission;

import java.time.LocalDateTime;

public record ApiPermissionResponse(
        Long id,
        String code,
        String name,
        String httpMethod,
        String pathPattern,
        String authority,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ApiPermissionResponse from(ApiPermission permission) {
        return new ApiPermissionResponse(
                permission.getId(), permission.getCode(), permission.getName(), permission.getHttpMethod(),
                permission.getPathPattern(), authorityOf(permission), permission.getStatus(),
                permission.getCreatedAt(), permission.getUpdatedAt()
        );
    }

    public static String authorityOf(ApiPermission permission) {
        return permission.getHttpMethod() + ":" + permission.getPathPattern();
    }
}
