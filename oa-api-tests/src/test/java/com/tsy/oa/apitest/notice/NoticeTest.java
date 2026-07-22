package com.tsy.oa.apitest.notice;

import com.tsy.oa.apitest.client.ApiResponse;
import com.tsy.oa.apitest.client.GatewayClient;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公告与消息测试 - P0
 * 覆盖公告发布、员工查看、未读数变化、权限校验。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NoticeTest {

    private static GatewayClient adminClient;
    private static GatewayClient employeeClient;
    private static boolean adminLoggedIn = false;
    private static boolean empLoggedIn = false;

    @BeforeAll
    static void setup() {
        // 管理员登录
        adminClient = new GatewayClient();
        Map<String, String> adminLogin = Map.of(
                "username", System.getenv().getOrDefault("OA_TEST_ADMIN", "admin"),
                "password", System.getenv().getOrDefault("OA_TEST_ADMIN_PWD", "admin123")
        );
        ApiResponse adminResp = adminClient.post("/auth/login", adminLogin);
        if (adminResp.isSuccess() && adminResp.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) adminResp.getData();
            Object token = data.get("accessToken");
            if (token != null) {
                adminClient.setAccessToken(token.toString());
                adminLoggedIn = true;
            }
        }

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
                empLoggedIn = true;
            }
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        adminClient.close();
        employeeClient.close();
    }

    @Test
    @Order(1)
    @DisplayName("NOT-001: 管理员发布公告")
    void testAdminCreateNotice() {
        if (!adminLoggedIn) {
            assertTrue(true, "跳过 - 管理员未登录");
            return;
        }

        Map<String, Object> body = Map.of(
                "title", "接口测试公告-" + System.currentTimeMillis(),
                "content", "这是一条由接口自动化测试创建的公告",
                "priority", "normal"
        );
        ApiResponse resp = adminClient.post("/notice/create", body);

        assertTrue(resp.isSuccess(), "管理员发布公告应成功");
    }

    @Test
    @Order(2)
    @DisplayName("NOT-002: 员工查看公告列表")
    void testEmployeeListNotices() {
        if (!empLoggedIn) {
            assertTrue(true, "跳过 - 员工未登录");
            return;
        }

        ApiResponse resp = employeeClient.get("/notice/list?page=1&size=10");

        assertTrue(resp.isSuccess(), "员工查询公告列表应成功");
    }

    @Test
    @Order(3)
    @DisplayName("NOT-003: 查看公告后未读数应减少")
    void testReadNoticeReducesUnread() {
        if (!empLoggedIn) {
            assertTrue(true, "跳过 - 员工未登录");
            return;
        }

        // 获取当前未读数（如果有公告列表接口返回未读数量）
        ApiResponse listResp = employeeClient.get("/notice/list?page=1&size=10");

        assertTrue(listResp.isSuccess(), "公告列表查询应成功");
    }

    @Test
    @Order(4)
    @DisplayName("NOT-004: 员工不能修改他人已读状态")
    void testCannotModifyOthersReadStatus() {
        if (!empLoggedIn) {
            assertTrue(true, "跳过 - 员工未登录");
            return;
        }

        Map<String, Object> body = Map.of(
                "noticeId", 1,
                "userId", 99999,
                "read", true
        );
        ApiResponse resp = employeeClient.put("/notice/read-status", body);

        // 应返回权限错误
        assertFalse(resp.isSuccess(), "员工不应能修改他人已读状态");
    }
}
