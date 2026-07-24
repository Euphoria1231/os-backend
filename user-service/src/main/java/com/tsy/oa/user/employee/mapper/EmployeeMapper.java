package com.tsy.oa.user.employee.mapper;

import com.tsy.oa.user.employee.model.Employee;
import com.tsy.oa.user.employee.dto.ApprovalRouteResponse;
import com.tsy.oa.user.dashboard.dto.NameCountResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    int insert(Employee employee);
    Employee findById(Long id);
    Employee findByUsername(String username);
    List<Employee> findAll();
    List<Employee> findByLeaderId(Long leaderId);
    ApprovalRouteResponse findApprovalRoute(Long applicantId);
    int update(Employee employee);
    int deleteById(Long id);
    int countByEmployeeNo(@Param("employeeNo") String employeeNo, @Param("excludeId") Long excludeId);
    int countByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);
    boolean hasRoleCode(@Param("employeeId") Long employeeId, @Param("roleCode") String roleCode);
    int countByLeaderId(Long leaderId);
    long countEmployees();
    long countEnabledEmployees();
    long countDisabledEmployees();
    List<NameCountResponse> countEmployeesByDepartment();
    List<NameCountResponse> countEmployeesByPosition();
}
