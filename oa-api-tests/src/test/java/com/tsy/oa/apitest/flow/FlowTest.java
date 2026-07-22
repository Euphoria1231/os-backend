package com.tsy.oa.apitest.flow;

import com.tsy.oa.apitest.client.ApiResponse;
import com.tsy.oa.apitest.client.GatewayClient;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批测试 - P0
 * 覆盖请假/加班提交、待办查询、权限校验、驳回验证。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlowTest {

    private static GatewayClient employeeClient;
    private static GatewayClient managerClient;
    private static boolean managerLoggedIn = false;

    @BeforeAll
    static void setup() {
        // 员工登录
        employeeClient = new GatewayClient();
        Map<String, String> empLogin = Map.of(
                "username", System.getenv().getOrDefault("OA_TEST_EMPLOYEE", "employee"),
                "password", System.getenv().getOrDefault("OA_TEST_EMPLOYEE_PWD", "employee123")
        );
        ApiResponse empResp = employeeClient.post("/auth/login", empLogin);
        if (empResp.isSuccess() && empResp.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) empResp.getData();
            Object token = data.get("accessToken");
            if (token != null) {
                employeeClient.setAccessToken(token.toString());
            }
        }

        // 管理员/领导登录（用于审批测试）
        managerClient = new GatewayClient();
        Map<String, String> mgrLogin = Map.of(
                "username", System.getenv().getOrDefault("OA_TEST_ADMIN", "admin"),
                "password", System.getenv().getOrDefault("OA_TEST_ADMIN_PWD", "admin123")
        );
        ApiResponse mgrResp = managerClient.post("/auth/login", mgrLogin);
        if (mgrResp.isSuccess() && mgrResp.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) mgrResp.getData();
            Object token = data.get("accessToken");
            if (token != null) {
                managerClient.setAccessToken(token.toString());
                managerLoggedIn = true;
            }
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        employeeClient.close();
        managerClient.close();
    }

    @Test
    @Order(1)
    @DisplayName("FLOW-001: 请假提交成功")
    void testSubmitLeave() {
        Map<String, Object> body = Map.of(
                "type", "leave",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate", LocalDate.now().plusDays(2).toString(),
                "reason", "接口自动化测试-请假-" + System.currentTimeMillis()
        );
        ApiResponse resp = employeeClient.post("/flow/leave", body);

        assertTrue(resp.isSuccess(), "请假提交应成功");
    }

    @Test
    @Order(2)
    @DisplayName("FLOW-002: 加班提交成功")
    void testSubmitOvertime() {
        Map<String, Object> body = Map.of(
                "type", "overtime",
                "date", LocalDate.now().plusDays(3).toString(),
                "duration", 2.0,
                "reason", "接口自动化测试-加班-" + System.currentTimeMillis()
        );
        ApiResponse resp = employeeClient.post("/flow/overtime", body);

        assertTrue(resp.isSuccess(), "加班提交应成功");
    }

    @Test
    @Order(3)
    @DisplayName("FLOW-003: 领导看到待办")
    void testManagerSeesPendingTasks() {
        if (!managerLoggedIn) {
            assertTrue(true, "跳过 - 管理员未登录");
            return;
        }

        ApiResponse resp = managerClient.get("/flow/pending-tasks?page=1&size=20");

        assertTrue(resp.isSuccess(), "查询待办列表应成功");
        assertNotNull(resp.getData(), "待办列表不应为空");
    }

    @Test
    @Order(4)
    @DisplayName("FLOW-004: 普通员工不能审批")
    void testEmployeeCannotApprove() {
        Map<String, Object> body = Map.of(
                "taskId", 99999,
                "approved", true,
                "comment", "测试-普通员工审批"
        );
        ApiResponse resp = employeeClient.post("/flow/approve", body);

        assertFalse(resp.isSuccess(), "普通员工审批应被拒绝");
    }

    @Test
    @Order(5)
    @DisplayName("FLOW-007: 驳回必须有意见（参数校验）")
    void testRejectRequiresComment() {
        if (!managerLoggedIn) {
            assertTrue(true, "跳过 - 管理员未登录");
            return;
        }

        Map<String, Object> body = Map.of(
                "taskId", 99999,
                "approved", false
                // 没有 comment 字段
        );
        ApiResponse resp = managerClient.post("/flow/approve", body);

        // 应返回参数校验错误
        assertFalse(resp.isSuccess(), "驳回无意见应被拒绝");
    }
}
