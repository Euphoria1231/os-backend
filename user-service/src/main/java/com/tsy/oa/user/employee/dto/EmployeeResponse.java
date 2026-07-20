package com.tsy.oa.user.employee.dto;

import com.tsy.oa.user.employee.model.Employee;

import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id, String employeeNo, String username, String realName,
        Long departmentId, String departmentName, Long positionId, String positionName,
        Long leaderId, String leaderName, String phone, String email, Integer status,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(), employee.getEmployeeNo(), employee.getUsername(), employee.getRealName(),
                employee.getDepartmentId(), employee.getDepartmentName(), employee.getPositionId(), employee.getPositionName(),
                employee.getLeaderId(), employee.getLeaderName(), employee.getPhone(), employee.getEmail(), employee.getStatus(),
                employee.getCreatedAt(), employee.getUpdatedAt()
        );
    }
}
