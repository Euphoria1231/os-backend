package com.tsy.oa.user.dashboard.service;

import com.tsy.oa.user.dashboard.dto.OrganizationStatisticsResponse;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationStatisticsService {

    private final EmployeeMapper employeeMapper;

    public OrganizationStatisticsService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Transactional(readOnly = true)
    public OrganizationStatisticsResponse getOrganizationStatistics() {
        return new OrganizationStatisticsResponse(
                employeeMapper.countEmployees(),
                employeeMapper.countEnabledEmployees(),
                employeeMapper.countDisabledEmployees(),
                employeeMapper.countEmployeesByDepartment(),
                employeeMapper.countEmployeesByPosition()
        );
    }
}
