package com.tsy.oa.flow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = FlowableProcessDefinitionTests.TestApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:oa_flow_process;MODE=LEGACY;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
)
class FlowableProcessDefinitionTests {

    private static final String LEAVE_APPROVAL_KEY = "leave-approval";
    private static final String OVERTIME_APPROVAL_KEY = "overtime-approval";

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Test
    void deploysLeaveAndOvertimeProcessDefinitions() {
        ProcessDefinition leaveDefinition = latestDefinition(LEAVE_APPROVAL_KEY);
        ProcessDefinition overtimeDefinition = latestDefinition(OVERTIME_APPROVAL_KEY);

        assertThat(leaveDefinition).isNotNull();
        assertThat(overtimeDefinition).isNotNull();
    }

    @Test
    void startsLeaveApprovalWithDirectLeaderAndSecondApproverTasks() {
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                LEAVE_APPROVAL_KEY,
                Map.of(
                        "applicantId", "10",
                        "directLeaderId", "20",
                        "secondApproverId", "30"
                )
        ).getId();

        Task directLeaderTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskCandidateUser("20")
                .singleResult();
        assertThat(directLeaderTask).isNotNull();
        assertThat(directLeaderTask.getName()).isEqualTo("直属领导审批");

        taskService.complete(directLeaderTask.getId(), Map.of("approved", true));

        Task secondApproverTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskCandidateUser("30")
                .singleResult();
        assertThat(secondApproverTask).isNotNull();
        assertThat(secondApproverTask.getName()).isEqualTo("二级审批");

        taskService.complete(secondApproverTask.getId(), Map.of("approved", true));

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult()).isNull();
    }

    @Test
    void startsOvertimeApprovalWithOnlyDirectLeaderTask() {
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                OVERTIME_APPROVAL_KEY,
                Map.of(
                        "applicantId", "10",
                        "directLeaderId", "20"
                )
        ).getId();

        Task directLeaderTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskCandidateUser("20")
                .singleResult();
        assertThat(directLeaderTask).isNotNull();
        assertThat(directLeaderTask.getName()).isEqualTo("直属领导审批");

        taskService.complete(directLeaderTask.getId(), Map.of("approved", true));

        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult()).isNull();
        assertThat(taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .count()).isZero();
    }

    private ProcessDefinition latestDefinition(String key) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.flow")
    static class TestApplication {
    }
}
