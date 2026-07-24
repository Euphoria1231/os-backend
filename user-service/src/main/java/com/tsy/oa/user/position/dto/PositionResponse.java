package com.tsy.oa.user.position.dto;

import com.tsy.oa.user.position.model.Position;

import java.time.LocalDateTime;

public record PositionResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String code,
        String name,
        String description,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(
                position.getId(),
                position.getDepartmentId(),
                position.getDepartmentName(),
                position.getCode(),
                position.getName(),
                position.getDescription(),
                position.getStatus(),
                position.getCreatedAt(),
                position.getUpdatedAt()
        );
    }
}
