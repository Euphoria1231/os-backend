package com.tsy.oa.user.department.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.department.dto.DepartmentRequest;
import com.tsy.oa.user.department.dto.DepartmentResponse;
import com.tsy.oa.user.department.mapper.DepartmentMapper;
import com.tsy.oa.user.department.model.Department;
import com.tsy.oa.user.error.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name, null);

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
}
