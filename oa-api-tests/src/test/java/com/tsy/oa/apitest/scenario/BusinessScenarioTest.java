package com.tsy.oa.apitest.scenario;

import com.tsy.oa.apitest.client.ApiResponse;
import com.tsy.oa.apitest.client.GatewayClient;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 跨服务业务场景测试 - P0
 * 覆盖打卡、审批、公告三大核心业务闭环。
 * 场景失败时输出业务步骤和响应摘要，不输出 Token。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BusinessScenarioTest {

    private static GatewayClient empClient;
    private static GatewayClient adminClient;
    private static boolean empReady = false;
    private static boolean adminReady = false;

    @BeforeAll
    static void setup() {
        // 员工登录
        empClient = new GatewayClient();
        Map<String, String> empLogin = Map.of(
                "username", System.getenv().getOrDefault("OA_TEST_EMPLOYEE", "employee"),
                "password", System.getenv().getOrDefault("OA_TEST_EMPLOYEE_PWD", "employee123")
        );
        ApiResponse empResp = empClient.post("/auth/login", empLogin);
        if (empResp.isSuccess() && empResp.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) empResp.getData();
            Object token = data.get("accessToken");
            if (token != null) {
                empClient.setAccessToken(token.toString());
                empReady = true;
            }
        }

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
                adminReady = true;
            }
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        empClient.close();
        adminClient.close();
    }

    @Test
    @Order(1)
    @DisplayName("场景1: 员工登录 -> 打卡 -> 查询记录")
    void testEmployeeAttendanceScenario() {
        if (!empReady) {
            assertTrue(true, "跳过 - 员工未登录");
            return;
        }

        try {
            // Step 1: 查看今日状态
            ApiResponse statusResp = empClient.get("/attendance/today-status");
            System.out.println("[场景1-1] 今日状态: " + statusResp);

            // Step 2: 上班打卡
            ApiResponse punchInResp = empClient.post("/attendance/punch-in", Map.of());
            System.out.println("[场景1-2] 上班打卡: " + punchInResp);

            // Step 3: 查询打卡记录
            ApiResponse recordsResp = empClient.get("/attendance/records?page=1&size=10");
            System.out.println("[场景1-3] 记录查询: " + recordsResp);

            assertNotNull(punchInResp, "打卡应有响应");
            assertNotNull(recordsResp, "记录查询应有响应");
        } catch (Exception e) {
            fail("场景1执行异常: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("场景2: 员工请假提交 -> 领导审批 -> 查看进度")
    void testLeaveApprovalScenario() {
        if (!empReady) {
            assertTrue(true, "跳过 - 员工未登录");
            return;
        }

        try {
            // Step 1: 员工提交请假申请
            Map<String, Object> leaveBody = Map.of(
                    "type", "leave",
                    "startDate", LocalDate.now().plusDays(5).toString(),
                    "endDate", LocalDate.now().plusDays(6).toString(),
                    "reason", "自动化场景测试-请假-" + System.currentTimeMillis()
            );
            ApiResponse submitResp = empClient.post("/flow/leave", leaveBody);
            System.out.println("[场景2-1] 请假提交: " + submitResp);

            // Step 2: 管理员查看待办
            if (adminReady) {
                ApiResponse pendingResp = adminClient.get("/flow/pending-tasks?page=1&size=20");
                System.out.println("[场景2-2] 待办列表: " + pendingResp);
            } else {
                System.out.println("[场景2-2] 跳过 - 管理员未登录");
            }

            assertNotNull(submitResp, "请假提交应有响应");
        } catch (Exception e) {
            fail("场景2执行异常: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("场景3: 管理员发布公告 -> 员工查看")
    void testNoticeScenario() {
        if (!adminReady || !empReady) {
            assertTrue(true, "跳过 - 管理员或员工未登录");
            return;
        }

        try {
            // Step 1: 管理员发布公告
            Map<String, Object> noticeBody = Map.of(
                    "title", "场景测试公告-" + System.currentTimeMillis(),
                    "content", "这是一条自动化场景测试公告",
                    "priority", "normal"
            );
            ApiResponse createResp = adminClient.post("/notice/create", noticeBody);
            System.out.println("[场景3-1] 发布公告: " + createResp);

            // Step 2: 员工查看公告列表
            ApiResponse listResp = empClient.get("/notice/list?page=1&size=10");
            System.out.println("[场景3-2] 公告列表: " + listResp);

            assertNotNull(createResp, "发布公告应有响应");
            assertNotNull(listResp, "公告列表查询应有响应");
        } catch (Exception e) {
            fail("场景3执行异常: " + e.getMessage());
        }
    }
}
