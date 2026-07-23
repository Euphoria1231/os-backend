package com.tsy.oa.user.employee.mapper;

import com.tsy.oa.user.employee.model.Employee;
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
    int update(Employee employee);
    int deleteById(Long id);
    int countByEmployeeNo(@Param("employeeNo") String employeeNo, @Param("excludeId") Long excludeId);
    int countByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);
    long countEmployees();
    long countEnabledEmployees();
    long countDisabledEmployees();
    List<NameCountResponse> countEmployeesByDepartment();
    List<NameCountResponse> countEmployeesByPosition();
}
