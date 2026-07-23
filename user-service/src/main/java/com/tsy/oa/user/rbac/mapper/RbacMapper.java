package com.tsy.oa.user.rbac.mapper;

import com.tsy.oa.user.rbac.model.ApiPermission;
import com.tsy.oa.user.rbac.model.Menu;
import com.tsy.oa.user.rbac.model.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RbacMapper {

    int insertRole(Role role);
    Role findRoleById(Long id);
    List<Role> findAllRoles();
    int updateRole(Role role);
    int deleteRoleById(Long id);
    int countRoleByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    int insertMenu(Menu menu);
    Menu findMenuById(Long id);
    List<Menu> findAllMenus();
    int updateMenu(Menu menu);
    int deleteMenuById(Long id);

    int insertApiPermission(ApiPermission permission);
    ApiPermission findApiPermissionById(Long id);
    List<ApiPermission> findAllApiPermissions();
    int updateApiPermission(ApiPermission permission);
    int deleteApiPermissionById(Long id);
    int countApiPermissionByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
    int deleteRoleMenus(Long roleId);
    int deleteMenuRoles(Long menuId);
    List<Long> findMenuIdsByRoleId(Long roleId);
    int insertRoleApiPermission(
            @Param("roleId") Long roleId,
            @Param("apiPermissionId") Long apiPermissionId
    );
    int deleteRoleApiPermissions(Long roleId);
    int deleteApiPermissionRoles(Long apiPermissionId);
    List<Long> findApiPermissionIdsByRoleId(Long roleId);
    int insertEmployeeRole(@Param("employeeId") Long employeeId, @Param("roleId") Long roleId);
    int deleteEmployeeRoles(Long employeeId);
    int deleteRoleEmployees(Long roleId);

    List<Role> findRolesByEmployeeId(Long employeeId);
    List<Menu> findMenusByEmployeeId(Long employeeId);
    List<ApiPermission> findApiPermissionsByEmployeeId(Long employeeId);
}
