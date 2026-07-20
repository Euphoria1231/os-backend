package com.tsy.oa.user.department.dto;

import com.tsy.oa.user.department.model.Department;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        Long parentId,
        String name,
        Long leaderEmployeeId,
        Integer sortOrder,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getParentId(),
                department.getName(),
                department.getLeaderEmployeeId(),
                department.getSortOrder(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
