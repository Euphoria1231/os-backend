package com.tsy.oa.user.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.user.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthControllerTests.BlacklistTestConfiguration.class)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepareEmployee() {
        jdbcTemplate.update("DELETE FROM business_operation_log");
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM department");
        jdbcTemplate.update("INSERT INTO department (id, parent_id, name, sort_order, status) VALUES (1, 0, '研发部', 1, 1)");
        jdbcTemplate.update("INSERT INTO position (id, code, name, status) VALUES (1, 'JAVA_DEV', 'Java开发工程师', 1)");
        insertEmployee(1);
    }

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("zhangsan", "Password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.employee.username").value("zhangsan"));

        assertEquals(1, logCount("LOGIN", "SUCCESS"));
        String summary = jdbcTemplate.queryForObject(
                "SELECT summary FROM business_operation_log WHERE operation_type = 'LOGIN'",
                String.class
        );
        assertFalse(summary.contains("Password123"));
    }

    @Test
    void rejectsWrongPasswordAndDisabledEmployee() throws Exception {
        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("zhangsan", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        jdbcTemplate.update("UPDATE employee SET status = 0 WHERE username = 'zhangsan'");

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("zhangsan", "Password123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        assertEquals(2, logCount("LOGIN", "FAILURE"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_operation_log WHERE summary LIKE '%wrong-password%'",
                Integer.class
        ));
    }

    @Test
    void currentUserWorksUntilTokenIsLoggedOut() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("zhangsan", "Password123")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loginBody = objectMapper.readTree(loginResponse);
        String token = loginBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/user/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("zhangsan"));

        mockMvc.perform(post("/api/user/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Employee-Id", 1L))
                .andExpect(status().isOk());

        assertEquals(1, logCount("LOGOUT", "SUCCESS"));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT operator_id FROM business_operation_log WHERE operation_type = 'LOGOUT'",
                Long.class
        ));

        mockMvc.perform(get("/api/user/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    private void insertEmployee(int status) {
        jdbcTemplate.update(
                "INSERT INTO employee (employee_no, username, password_hash, real_name, department_id, position_id, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "E001",
                "zhangsan",
                passwordEncoder.encode("Password123"),
                "张三",
                1L,
                1L,
                status
        );
    }

    private String loginJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginPayload(username, password));
    }

    private int logCount(String operationType, String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_operation_log WHERE operation_type = ? AND operation_status = ?",
                Integer.class,
                operationType,
                status
        );
    }

    private record LoginPayload(String username, String password) {
    }

    @TestConfiguration
    static class BlacklistTestConfiguration {

        @Bean
        @Primary
        TokenBlacklistService inMemoryTokenBlacklistService() {
            return new TokenBlacklistService() {
                private final Set<String> tokenIds = ConcurrentHashMap.newKeySet();

                @Override
                public void blacklist(String tokenId, Duration ttl) {
                    tokenIds.add(tokenId);
                }

                @Override
                public boolean isBlacklisted(String tokenId) {
                    return tokenIds.contains(tokenId);
                }
            };
        }
    }
}
