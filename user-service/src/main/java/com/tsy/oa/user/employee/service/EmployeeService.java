package com.tsy.oa.user.employee.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.department.mapper.DepartmentMapper;
import com.tsy.oa.user.employee.dto.EmployeeCreateRequest;
import com.tsy.oa.user.employee.dto.EmployeeResponse;
import com.tsy.oa.user.employee.dto.EmployeeUpdateRequest;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import com.tsy.oa.user.employee.model.Employee;
import com.tsy.oa.user.error.UserErrorCode;
import com.tsy.oa.user.position.service.PositionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionService positionService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(
            EmployeeMapper employeeMapper,
            DepartmentMapper departmentMapper,
            PositionService positionService,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
        this.positionService = positionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        ensureEmployeeNoAvailable(request.employeeNo(), null);
        ensureUsernameAvailable(request.username(), null);
        validateOrganization(request.departmentId(), request.positionId(), request.leaderId());

        Employee employee = new Employee();
        employee.setEmployeeNo(request.employeeNo().trim());
        employee.setUsername(request.username().trim());
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        employee.setRealName(request.realName().trim());
        employee.setDepartmentId(request.departmentId());
        employee.setPositionId(request.positionId());
        employee.setLeaderId(request.leaderId());
        employee.setPhone(request.phone());
        employee.setEmail(request.email());
        employee.setStatus(request.status());
        employeeMapper.insert(employee);
        return getById(employee.getId());
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(requireEmployee(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        return employeeMapper.findAll().stream().map(EmployeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listDirectReports(Long leaderId) {
        return employeeMapper.findByLeaderId(leaderId).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee existing = requireEmployee(id);
        validateOrganization(request.departmentId(), request.positionId(), request.leaderId());
        existing.setRealName(request.realName().trim());
        existing.setDepartmentId(request.departmentId());
        existing.setPositionId(request.positionId());
        existing.setLeaderId(request.leaderId());
        existing.setPhone(request.phone());
        existing.setEmail(request.email());
        existing.setStatus(request.status());
        employeeMapper.update(existing);
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        requireEmployee(id);
        employeeMapper.deleteById(id);
    }

    public Employee requireEmployee(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new BusinessException(UserErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employee;
    }

    private void validateOrganization(Long departmentId, Long positionId, Long leaderId) {
        if (departmentMapper.findById(departmentId) == null) {
            throw new BusinessException(UserErrorCode.DEPARTMENT_NOT_FOUND);
        }
        positionService.requirePosition(positionId);
        if (leaderId != null) {
            requireEmployee(leaderId);
        }
    }

    private void ensureEmployeeNoAvailable(String employeeNo, Long excludeId) {
        if (employeeMapper.countByEmployeeNo(employeeNo.trim(), excludeId) > 0) {
            throw new BusinessException(UserErrorCode.EMPLOYEE_NO_EXISTS);
        }
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        if (employeeMapper.countByUsername(username.trim(), excludeId) > 0) {
            throw new BusinessException(UserErrorCode.USERNAME_EXISTS);
        }
    }
}
