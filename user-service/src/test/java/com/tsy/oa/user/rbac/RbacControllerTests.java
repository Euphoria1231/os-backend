package com.tsy.oa.user.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.common.security.JwtClaims;
import com.tsy.oa.common.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM business_operation_log");
        jdbcTemplate.update("DELETE FROM role_api_permission");
        jdbcTemplate.update("DELETE FROM role_menu");
        jdbcTemplate.update("DELETE FROM employee_role");
        jdbcTemplate.update("DELETE FROM sys_api_permission");
        jdbcTemplate.update("DELETE FROM sys_menu");
        jdbcTemplate.update("DELETE FROM sys_role");
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM department");
        jdbcTemplate.update("INSERT INTO department (id, parent_id, name, sort_order, status) VALUES (1, 0, '研发部', 1, 1)");
        jdbcTemplate.update("INSERT INTO position (id, code, name, status) VALUES (1, 'JAVA_DEV', 'Java开发工程师', 1)");
        jdbcTemplate.update(
                "INSERT INTO employee (id, employee_no, username, password_hash, real_name, department_id, position_id, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "E001", "zhangsan", passwordEncoder.encode("Password123"), "张三", 1L, 1L, 1
        );
    }

    @Test
    void managesRoleMenuAndApiPermissionCrud() throws Exception {
        long roleId = createAndReadId("/api/user/roles", roleJson("ADMIN", "系统管理员"));
        mockMvc.perform(put("/api/user/roles/{id}", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleJson("ADMIN", "平台管理员")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("平台管理员"));

        long menuId = createAndReadId("/api/user/menus", menuJson("员工管理", "/employees"));
        mockMvc.perform(get("/api/user/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(menuId));

        long apiPermissionId = createAndReadId(
                "/api/user/api-permissions",
                apiPermissionJson("USER_EMPLOYEE_READ", "GET", "/api/user/employees/**")
        );
        mockMvc.perform(get("/api/user/api-permissions/{id}", apiPermissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("USER_EMPLOYEE_READ"));

        mockMvc.perform(delete("/api/user/api-permissions/{id}", apiPermissionId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/user/menus/{id}", menuId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/user/roles/{id}", roleId))
                .andExpect(status().isOk());
    }

    @Test
    void assignsRoleGrantsAndEmployeeRoles() throws Exception {
        long roleId = createAndReadId("/api/user/roles", roleJson("ADMIN", "系统管理员"));
        long menuId = createAndReadId("/api/user/menus", menuJson("员工管理", "/employees"));
        long apiPermissionId = createAndReadId(
                "/api/user/api-permissions",
                apiPermissionJson("USER_EMPLOYEE_READ", "GET", "/api/user/employees/**")
        );

        mockMvc.perform(put("/api/user/roles/{id}/permissions", roleId)
                        .header("X-Employee-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleGrantPayload(
                                List.of(menuId), List.of(apiPermissionId)
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/user/employees/{id}/roles", 1L)
                        .header("X-Employee-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeRolePayload(List.of(roleId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/employees/{id}/permissions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0].code").value("ADMIN"))
                .andExpect(jsonPath("$.data.menus[0].path").value("/employees"))
                .andExpect(jsonPath("$.data.apiPermissions[0].authority").value("GET:/api/user/employees/**"));

        assertEquals(1, logCount("ASSIGN_ROLE_PERMISSIONS", "SUCCESS"));
        assertEquals(1, logCount("ASSIGN_EMPLOYEE_ROLES", "SUCCESS"));
    }

    @Test
    void rejectsReplacingSuperAdminPermissionsAndPreservesCurrentGrant() throws Exception {
        long roleId = createAndReadId("/api/user/roles", roleJson("SUPER_ADMIN", "超级管理员"));
        long menuId = createAndReadId("/api/user/menus", menuJson("员工管理", "/employees"));
        long apiPermissionId = createAndReadId(
                "/api/user/api-permissions",
                apiPermissionJson("USER_EMPLOYEE_READ", "GET", "/api/user/employees/**")
        );
        jdbcTemplate.update("INSERT INTO role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        jdbcTemplate.update(
                "INSERT INTO role_api_permission (role_id, api_permission_id) VALUES (?, ?)",
                roleId,
                apiPermissionId
        );

        mockMvc.perform(put("/api/user/roles/{id}/permissions", roleId)
                        .header("X-Employee-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleGrantPayload(List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40907))
                .andExpect(jsonPath("$.message").value("超级管理员始终拥有全部权限，不允许覆盖授权"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_menu WHERE role_id = ? AND menu_id = ?",
                Integer.class,
                roleId,
                menuId
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_api_permission WHERE role_id = ? AND api_permission_id = ?",
                Integer.class,
                roleId,
                apiPermissionId
        ));
    }

    @Test
    void returnsCurrentRoleGrantIds() throws Exception {
        long roleId = createAndReadId("/api/user/roles", roleJson("ADMIN", "系统管理员"));
        long menuId = createAndReadId("/api/user/menus", menuJson("员工管理", "/employees"));
        long apiPermissionId = createAndReadId(
                "/api/user/api-permissions",
                apiPermissionJson("USER_EMPLOYEE_READ", "GET", "/api/user/employees/**")
        );
        mockMvc.perform(put("/api/user/roles/{id}/permissions", roleId)
                        .header("X-Employee-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleGrantPayload(
                                List.of(menuId), List.of(apiPermissionId)
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/roles/{id}/permissions", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuIds[0]").value(menuId))
                .andExpect(jsonPath("$.data.apiPermissionIds[0]").value(apiPermissionId));
    }

    @Test
    void loginTokenContainsAssignedRolesAndApiPermissions() throws Exception {
        jdbcTemplate.update("INSERT INTO sys_role (id, code, name, status) VALUES (1, 'ADMIN', '系统管理员', 1)");
        jdbcTemplate.update(
                "INSERT INTO sys_api_permission (id, code, name, http_method, path_pattern, status) "
                        + "VALUES (1, 'USER_EMPLOYEE_READ', '员工查询', 'GET', '/api/user/employees/**', 1)"
        );
        jdbcTemplate.update("INSERT INTO employee_role (employee_id, role_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO role_api_permission (role_id, api_permission_id) VALUES (1, 1)");

        String response = mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        JwtClaims claims = jwtTokenService.parseToken(body.path("data").path("accessToken").asText());

        assertEquals(List.of("ADMIN"), claims.roles());
        assertEquals(List.of("GET:/api/user/employees/**"), claims.permissions());
    }

    private long createAndReadId(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .header("X-Employee-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private int logCount(String operationType, String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_operation_log WHERE operation_type = ? AND operation_status = ?",
                Integer.class,
                operationType,
                status
        );
    }

    private String roleJson(String code, String name) throws Exception {
        return objectMapper.writeValueAsString(new RolePayload(code, name, 1));
    }

    private String menuJson(String name, String path) throws Exception {
        return objectMapper.writeValueAsString(new MenuPayload(
                0L, name, path, "EmployeePage", "user:employee:view", "MENU", 1, 1
        ));
    }

    private String apiPermissionJson(String code, String method, String pathPattern) throws Exception {
        return objectMapper.writeValueAsString(new ApiPermissionPayload(
                code, "员工查询", method, pathPattern, 1
        ));
    }

    private record RolePayload(String code, String name, Integer status) {
    }

    private record MenuPayload(
            Long parentId,
            String name,
            String path,
            String component,
            String permission,
            String type,
            Integer sortOrder,
            Integer status
    ) {
    }

    private record ApiPermissionPayload(
            String code,
            String name,
            String httpMethod,
            String pathPattern,
            Integer status
    ) {
    }

    private record RoleGrantPayload(List<Long> menuIds, List<Long> apiPermissionIds) {
    }

    private record EmployeeRolePayload(List<Long> roleIds) {
    }
}
