package com.tsy.oa.user.rbac.dto;

import com.tsy.oa.user.rbac.model.Role;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String code,
        String name,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(), role.getCode(), role.getName(), role.getStatus(),
                role.getCreatedAt(), role.getUpdatedAt()
        );
    }
}
