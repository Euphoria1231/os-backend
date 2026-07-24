package com.tsy.oa.user.position.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.department.mapper.DepartmentMapper;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import com.tsy.oa.user.error.UserErrorCode;
import com.tsy.oa.user.position.dto.PositionRequest;
import com.tsy.oa.user.position.dto.PositionResponse;
import com.tsy.oa.user.position.mapper.PositionMapper;
import com.tsy.oa.user.position.model.Position;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PositionService {

    private final PositionMapper positionMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    public PositionService(
            PositionMapper positionMapper,
            DepartmentMapper departmentMapper,
            EmployeeMapper employeeMapper
    ) {
        this.positionMapper = positionMapper;
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
    }

    @Transactional
    public PositionResponse create(PositionRequest request) {
        String code = normalizeCode(request.code());
        ensureCodeAvailable(code, null);
        requireDepartment(request.departmentId());
        Position position = toPosition(request, code);
        positionMapper.insert(position);
        return getById(position.getId());
    }

    @Transactional(readOnly = true)
    public PositionResponse getById(Long id) {
        return PositionResponse.from(requirePosition(id));
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> list() {
        return positionMapper.findAll().stream().map(PositionResponse::from).toList();
    }

    @Transactional
    public PositionResponse update(Long id, PositionRequest request) {
        requirePosition(id);
        String code = normalizeCode(request.code());
        ensureCodeAvailable(code, id);
        requireDepartment(request.departmentId());
        if (positionMapper.countEmployeesOutsideDepartment(id, request.departmentId()) > 0) {
            throw new BusinessException(UserErrorCode.POSITION_DEPARTMENT_CHANGE_CONFLICT);
        }
        Position position = toPosition(request, code);
        position.setId(id);
        positionMapper.update(position);
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        requirePosition(id);
        if (employeeMapper.countByPositionId(id) > 0) {
            throw new BusinessException(UserErrorCode.POSITION_DELETE_CONFLICT);
        }
        positionMapper.deleteById(id);
    }

    public Position requirePosition(Long id) {
        Position position = positionMapper.findById(id);
        if (position == null) {
            throw new BusinessException(UserErrorCode.POSITION_NOT_FOUND);
        }
        return position;
    }

    private Position toPosition(PositionRequest request, String code) {
        Position position = new Position();
        position.setDepartmentId(request.departmentId());
        position.setCode(code);
        position.setName(request.name().trim());
        position.setDescription(request.description());
        position.setStatus(request.status());
        return position;
    }

    private void ensureCodeAvailable(String code, Long excludeId) {
        if (positionMapper.countByCode(code, excludeId) > 0) {
            throw new BusinessException(UserErrorCode.POSITION_CODE_EXISTS);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void requireDepartment(Long departmentId) {
        if (departmentMapper.findById(departmentId) == null) {
            throw new BusinessException(UserErrorCode.DEPARTMENT_NOT_FOUND);
        }
    }
}
