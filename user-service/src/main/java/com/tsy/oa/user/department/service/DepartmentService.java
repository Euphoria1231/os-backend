package com.tsy.oa.user.department.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.department.dto.DepartmentRequest;
import com.tsy.oa.user.department.dto.DepartmentResponse;
import com.tsy.oa.user.department.mapper.DepartmentMapper;
import com.tsy.oa.user.department.model.Department;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import com.tsy.oa.user.employee.model.Employee;
import com.tsy.oa.user.error.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DepartmentService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    public DepartmentService(DepartmentMapper departmentMapper, EmployeeMapper employeeMapper) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name, null);
        validateLeader(null, request.leaderEmployeeId());

        Department department = toDepartment(request, name);
        departmentMapper.insert(department);
        return getById(department.getId());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department department = departmentMapper.findById(id);
        if (department == null) {
            throw new BusinessException(UserErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return DepartmentResponse.from(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return departmentMapper.findAll().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        getById(id);
        String name = request.name().trim();
        ensureNameAvailable(name, id);
        validateLeader(id, request.leaderEmployeeId());

        Department department = toDepartment(request, name);
        department.setId(id);
        departmentMapper.update(department);
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        departmentMapper.deleteById(id);
    }

    private Department toDepartment(DepartmentRequest request, String name) {
        Department department = new Department();
        department.setParentId(request.parentId());
        department.setName(name);
        department.setLeaderEmployeeId(request.leaderEmployeeId());
        department.setSortOrder(request.sortOrder());
        department.setStatus(request.status());
        return department;
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        if (departmentMapper.countByName(name, excludeId) > 0) {
            throw new BusinessException(UserErrorCode.DEPARTMENT_NAME_EXISTS);
        }
    }

    private void validateLeader(Long departmentId, Long leaderEmployeeId) {
        if (leaderEmployeeId == null) {
            return;
        }
        Employee leader = employeeMapper.findById(leaderEmployeeId);
        if (leader == null
                || !Objects.equals(departmentId, leader.getDepartmentId())
                || !Integer.valueOf(1).equals(leader.getStatus())) {
            throw new BusinessException(UserErrorCode.DEPARTMENT_LEADER_DEPARTMENT_MISMATCH);
        }
        if (employeeMapper.hasRoleCode(leaderEmployeeId, SUPER_ADMIN_ROLE)) {
            throw new BusinessException(UserErrorCode.SUPER_ADMIN_CANNOT_BE_BUSINESS_LEADER);
        }
    }
}
