package com.tsy.oa.user.rbac.dto;

import com.tsy.oa.user.rbac.model.Menu;

import java.time.LocalDateTime;

public record MenuResponse(
        Long id,
        Long parentId,
        String name,
        String path,
        String component,
        String permission,
        String type,
        Integer sortOrder,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(), menu.getParentId(), menu.getName(), menu.getPath(), menu.getComponent(),
                menu.getPermission(), menu.getType(), menu.getSortOrder(), menu.getStatus(),
                menu.getCreatedAt(), menu.getUpdatedAt()
        );
    }
}
