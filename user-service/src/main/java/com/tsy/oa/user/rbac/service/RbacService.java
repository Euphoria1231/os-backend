package com.tsy.oa.user.rbac.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import com.tsy.oa.user.error.UserErrorCode;
import com.tsy.oa.user.rbac.dto.ApiPermissionRequest;
import com.tsy.oa.user.rbac.dto.ApiPermissionResponse;
import com.tsy.oa.user.rbac.dto.EmployeeAuthorizationResponse;
import com.tsy.oa.user.rbac.dto.EmployeeRoleRequest;
import com.tsy.oa.user.rbac.dto.MenuRequest;
import com.tsy.oa.user.rbac.dto.MenuResponse;
import com.tsy.oa.user.rbac.dto.RoleGrantRequest;
import com.tsy.oa.user.rbac.dto.RoleGrantResponse;
import com.tsy.oa.user.rbac.dto.RoleRequest;
import com.tsy.oa.user.rbac.dto.RoleResponse;
import com.tsy.oa.user.rbac.mapper.RbacMapper;
import com.tsy.oa.user.rbac.model.ApiPermission;
import com.tsy.oa.user.rbac.model.Menu;
import com.tsy.oa.user.rbac.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class RbacService {

    private final RbacMapper rbacMapper;
    private final EmployeeMapper employeeMapper;

    public RbacService(RbacMapper rbacMapper, EmployeeMapper employeeMapper) {
        this.rbacMapper = rbacMapper;
        this.employeeMapper = employeeMapper;
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        String code = normalizeCode(request.code());
        ensureRoleCodeAvailable(code, null);
        Role role = toRole(request, code);
        rbacMapper.insertRole(role);
        return getRole(role.getId());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        return RoleResponse.from(requireRole(id));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return rbacMapper.findAllRoles().stream().map(RoleResponse::from).toList();
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        requireRole(id);
        String code = normalizeCode(request.code());
        ensureRoleCodeAvailable(code, id);
        Role role = toRole(request, code);
        role.setId(id);
        rbacMapper.updateRole(role);
        return getRole(id);
    }

    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        rbacMapper.deleteRoleMenus(id);
        rbacMapper.deleteRoleApiPermissions(id);
        rbacMapper.deleteRoleEmployees(id);
        rbacMapper.deleteRoleById(id);
    }

    @Transactional
    public MenuResponse createMenu(MenuRequest request) {
        validateParentMenu(request.parentId(), null);
        Menu menu = toMenu(request);
        rbacMapper.insertMenu(menu);
        return getMenu(menu.getId());
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenu(Long id) {
        return MenuResponse.from(requireMenu(id));
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> listMenus() {
        return rbacMapper.findAllMenus().stream().map(MenuResponse::from).toList();
    }

    @Transactional
    public MenuResponse updateMenu(Long id, MenuRequest request) {
        requireMenu(id);
        validateParentMenu(request.parentId(), id);
        Menu menu = toMenu(request);
        menu.setId(id);
        rbacMapper.updateMenu(menu);
        return getMenu(id);
    }

    @Transactional
    public void deleteMenu(Long id) {
        requireMenu(id);
        rbacMapper.deleteMenuRoles(id);
        rbacMapper.deleteMenuById(id);
    }

    @Transactional
    public ApiPermissionResponse createApiPermission(ApiPermissionRequest request) {
        String code = normalizeCode(request.code());
        ensureApiPermissionCodeAvailable(code, null);
        ApiPermission permission = toApiPermission(request, code);
        rbacMapper.insertApiPermission(permission);
        return getApiPermission(permission.getId());
    }

    @Transactional(readOnly = true)
    public ApiPermissionResponse getApiPermission(Long id) {
        return ApiPermissionResponse.from(requireApiPermission(id));
    }

    @Transactional(readOnly = true)
    public List<ApiPermissionResponse> listApiPermissions() {
        return rbacMapper.findAllApiPermissions().stream().map(ApiPermissionResponse::from).toList();
    }

    @Transactional
    public ApiPermissionResponse updateApiPermission(Long id, ApiPermissionRequest request) {
        requireApiPermission(id);
        String code = normalizeCode(request.code());
        ensureApiPermissionCodeAvailable(code, id);
        ApiPermission permission = toApiPermission(request, code);
        permission.setId(id);
        rbacMapper.updateApiPermission(permission);
        return getApiPermission(id);
    }

    @Transactional
    public void deleteApiPermission(Long id) {
        requireApiPermission(id);
        rbacMapper.deleteApiPermissionRoles(id);
        rbacMapper.deleteApiPermissionById(id);
    }

    @Transactional
    public void assignRolePermissions(Long roleId, RoleGrantRequest request) {
        requireRole(roleId);
        List<Long> menuIds = request.menuIds().stream().distinct().toList();
        List<Long> apiPermissionIds = request.apiPermissionIds().stream().distinct().toList();
        menuIds.forEach(this::requireMenu);
        apiPermissionIds.forEach(this::requireApiPermission);

        rbacMapper.deleteRoleMenus(roleId);
        rbacMapper.deleteRoleApiPermissions(roleId);
        menuIds.forEach(menuId -> rbacMapper.insertRoleMenu(roleId, menuId));
        apiPermissionIds.forEach(permissionId -> rbacMapper.insertRoleApiPermission(roleId, permissionId));
    }

    @Transactional(readOnly = true)
    public RoleGrantResponse getRolePermissions(Long roleId) {
        requireRole(roleId);
        return new RoleGrantResponse(
                rbacMapper.findMenuIdsByRoleId(roleId),
                rbacMapper.findApiPermissionIdsByRoleId(roleId)
        );
    }

    @Transactional
    public void assignEmployeeRoles(Long employeeId, EmployeeRoleRequest request) {
        if (employeeMapper.findById(employeeId) == null) {
            throw new BusinessException(UserErrorCode.EMPLOYEE_NOT_FOUND);
        }
        List<Long> roleIds = request.roleIds().stream().distinct().toList();
        roleIds.forEach(this::requireRole);
        rbacMapper.deleteEmployeeRoles(employeeId);
        roleIds.forEach(roleId -> rbacMapper.insertEmployeeRole(employeeId, roleId));
    }

    @Transactional(readOnly = true)
    public EmployeeAuthorizationResponse getEmployeeAuthorization(Long employeeId) {
        if (employeeMapper.findById(employeeId) == null) {
            throw new BusinessException(UserErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return new EmployeeAuthorizationResponse(
                rbacMapper.findRolesByEmployeeId(employeeId).stream().map(RoleResponse::from).toList(),
                rbacMapper.findMenusByEmployeeId(employeeId).stream().map(MenuResponse::from).toList(),
                rbacMapper.findApiPermissionsByEmployeeId(employeeId).stream()
                        .map(ApiPermissionResponse::from)
                        .toList()
        );
    }

    private Role requireRole(Long id) {
        Role role = rbacMapper.findRoleById(id);
        if (role == null) {
            throw new BusinessException(UserErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private Menu requireMenu(Long id) {
        Menu menu = rbacMapper.findMenuById(id);
        if (menu == null) {
            throw new BusinessException(UserErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    private ApiPermission requireApiPermission(Long id) {
        ApiPermission permission = rbacMapper.findApiPermissionById(id);
        if (permission == null) {
            throw new BusinessException(UserErrorCode.API_PERMISSION_NOT_FOUND);
        }
        return permission;
    }

    private void ensureRoleCodeAvailable(String code, Long excludeId) {
        if (rbacMapper.countRoleByCode(code, excludeId) > 0) {
            throw new BusinessException(UserErrorCode.ROLE_CODE_EXISTS);
        }
    }

    private void ensureApiPermissionCodeAvailable(String code, Long excludeId) {
        if (rbacMapper.countApiPermissionByCode(code, excludeId) > 0) {
            throw new BusinessException(UserErrorCode.API_PERMISSION_CODE_EXISTS);
        }
    }

    private void validateParentMenu(Long parentId, Long currentMenuId) {
        if (parentId == 0) {
            return;
        }
        if (parentId.equals(currentMenuId)) {
            throw new BusinessException(UserErrorCode.MENU_NOT_FOUND);
        }
        requireMenu(parentId);
    }

    private Role toRole(RoleRequest request, String code) {
        Role role = new Role();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setStatus(request.status());
        return role;
    }

    private Menu toMenu(MenuRequest request) {
        Menu menu = new Menu();
        menu.setParentId(request.parentId());
        menu.setName(request.name().trim());
        menu.setPath(normalizeNullable(request.path()));
        menu.setComponent(normalizeNullable(request.component()));
        menu.setPermission(normalizeNullable(request.permission()));
        menu.setType(request.type().trim().toUpperCase(Locale.ROOT));
        menu.setSortOrder(request.sortOrder());
        menu.setStatus(request.status());
        return menu;
    }

    private ApiPermission toApiPermission(ApiPermissionRequest request, String code) {
        ApiPermission permission = new ApiPermission();
        permission.setCode(code);
        permission.setName(request.name().trim());
        permission.setHttpMethod(request.httpMethod().trim().toUpperCase(Locale.ROOT));
        permission.setPathPattern(request.pathPattern().trim());
        permission.setStatus(request.status());
        return permission;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
