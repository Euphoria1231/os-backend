package com.tsy.oa.apitest.auth;

import com.tsy.oa.apitest.client.ApiResponse;
import com.tsy.oa.apitest.client.GatewayClient;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证与安全测试 - P0
 * 覆盖登录成功、错误密码、禁用账号、不存在账号、无Token、篡改Token、退出后Token失效。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthTest {

    private static GatewayClient client;

    @BeforeAll
    static void setup() {
        client = new GatewayClient();
    }

    @AfterAll
    static void teardown() throws Exception {
        client.close();
    }

    @Test
    @Order(1)
    @DisplayName("AUTH-001: 正确账号密码登录成功")
    void testLoginSuccess() {
        Map<String, String> body = Map.of(
                "username", System.getenv().getOrDefault("OA_TEST_ADMIN", "admin"),
                "password", System.getenv().getOrDefault("OA_TEST_ADMIN_PWD", "admin123")
        );
        ApiResponse resp = client.post("/auth/login", body);

        assertAll(
                () -> assertTrue(resp.isSuccess(), "登录应返回成功"),
                () -> assertNotNull(resp.getData(), "响应应包含用户数据和Token")
        );
    }

    @Test
    @Order(2)
    @DisplayName("AUTH-002: 错误密码登录失败")
    void testLoginWrongPassword() {
        Map<String, String> body = Map.of(
                "username", "admin",
                "password", "wrong_password_123"
        );
        ApiResponse resp = client.post("/auth/login", body);

        assertFalse(resp.isSuccess(), "错误密码应登录失败");
    }

    @Test
    @Order(3)
    @DisplayName("AUTH-003: 不存在账号登录失败")
    void testLoginNonExistentUser() {
        Map<String, String> body = Map.of(
                "username", "nonexistent_user_xyz",
                "password", "somepass"
        );
        ApiResponse resp = client.post("/auth/login", body);

        assertFalse(resp.isSuccess(), "不存在的用户应登录失败");
    }

    @Test
    @Order(4)
    @DisplayName("AUTH-005: 无Token访问被拒绝")
    void testNoTokenRejected() {
        // 使用新客户端（未设置Token）
        GatewayClient anonClient = new GatewayClient();
        ApiResponse resp = anonClient.get("/user/employees");

        assertFalse(resp.isSuccess(), "无Token请求应被拒绝");
        // HTTP 401
        assertEquals(401, resp.getCode(), "应返回401状态码");
    }

    @Test
    @Order(5)
    @DisplayName("AUTH-006: 篡改Token被拒绝")
    void testTamperedTokenRejected() {
        GatewayClient tamperedClient = new GatewayClient();
        tamperedClient.setAccessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tampered.signature");

        ApiResponse resp = tamperedClient.get("/user/employees");

        assertFalse(resp.isSuccess(), "篡改Token应被拒绝");
    }

    @Test
    @Order(6)
    @DisplayName("AUTH-008: 退出后Token失效")
    void testLogoutThenTokenInvalid() {
        // 先登录
        Map<String, String> loginBody = Map.of(
                "username", System.getenv().getOrDefault("OA_TEST_ADMIN", "admin"),
                "password", System.getenv().getOrDefault("OA_TEST_ADMIN_PWD", "admin123")
        );
        ApiResponse loginResp = client.post("/auth/login", loginBody);
        assertTrue(loginResp.isSuccess(), "登录应先成功");

        // 退出
        ApiResponse logoutResp = client.post("/auth/logout", null);
        assertTrue(logoutResp.isSuccess(), "退出应成功");

        // 使用原Token请求
        ApiResponse afterLogout = client.get("/user/employees");
        assertFalse(afterLogout.isSuccess(), "退出后原Token应失效");
    }
}
