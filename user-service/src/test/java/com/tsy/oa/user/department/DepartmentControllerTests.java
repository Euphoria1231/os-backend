package com.tsy.oa.user.department;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDepartments() {
        jdbcTemplate.update("DELETE FROM department");
    }

    @Test
    void createsReadsAndListsDepartments() throws Exception {
        long departmentId = createDepartment("研发部");
        createDepartment("产品部");

        mockMvc.perform(get("/api/user/departments/{id}", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("研发部"));

        mockMvc.perform(get("/api/user/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updatesAndDeletesDepartment() throws Exception {
        long departmentId = createDepartment("研发部");

        mockMvc.perform(put("/api/user/departments/{id}", departmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson("技术研发部")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("技术研发部"));

        mockMvc.perform(delete("/api/user/departments/{id}", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/user/departments/{id}", departmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void rejectsBlankAndDuplicateDepartmentNames() throws Exception {
        mockMvc.perform(post("/api/user/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        createDepartment("研发部");

        mockMvc.perform(post("/api/user/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson("研发部")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    void unknownEndpointReturnsNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/user/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    private long createDepartment(String name) throws Exception {
        String response = mockMvc.perform(post("/api/user/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("id").asLong();
    }

    private String departmentJson(String name) throws Exception {
        return objectMapper.writeValueAsString(new DepartmentPayload(
                0L,
                name,
                null,
                1,
                1
        ));
    }

    private record DepartmentPayload(
            Long parentId,
            String name,
            Long leaderEmployeeId,
            Integer sortOrder,
            Integer status
    ) {
    }
}
