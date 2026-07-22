package com.tsy.oa.flow.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.flow.dto.ApplicationRequest;
import com.tsy.oa.flow.dto.ApprovalRequest;
import com.tsy.oa.flow.dto.ApprovalTaskResponse;
import com.tsy.oa.flow.dto.FlowApplicationResponse;
import com.tsy.oa.flow.employee.EmployeeDirectory;
import com.tsy.oa.flow.error.FlowErrorCode;
import com.tsy.oa.flow.event.ApprovalCompletedEvent;
import com.tsy.oa.flow.event.ApprovalEventPublisher;
import com.tsy.oa.flow.mapper.FlowMapper;
import com.tsy.oa.flow.model.FlowApplication;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FlowService {

    private static final Logger log = LoggerFactory.getLogger(FlowService.class);

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String LEAVE = "LEAVE";
    private static final String OVERTIME = "OVERTIME";
    private static final String APPROVE = "APPROVE";
    private static final String REJECT = "REJECT";
    private static final String LEAVE_APPROVAL_KEY = "leave-approval";
    private static final String OVERTIME_APPROVAL_KEY = "overtime-approval";

    private final FlowMapper flowMapper;
    private final EmployeeDirectory employeeDirectory;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ApprovalEventPublisher approvalEventPublisher;

    public FlowService(
            FlowMapper flowMapper,
            EmployeeDirectory employeeDirectory,
            RuntimeService runtimeService,
            TaskService taskService,
            ApprovalEventPublisher approvalEventPublisher
    ) {
        this.flowMapper = flowMapper;
        this.employeeDirectory = employeeDirectory;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.approvalEventPublisher = approvalEventPublisher;
    }

    @Transactional
    public FlowApplicationResponse submitLeave(Long applicantId, ApplicationRequest request) {
        return submit(applicantId, LEAVE, request);
    }

    @Transactional
    public FlowApplicationResponse submitOvertime(Long applicantId, ApplicationRequest request) {
        return submit(applicantId, OVERTIME, request);
    }

    @Transactional(readOnly = true)
    public List<FlowApplicationResponse> listMyApplications(Long applicantId) {
        return flowMapper.findApplicationsByApplicant(applicantId).stream()
                .map(FlowApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlowApplicationResponse> listTodo(Long approverId) {
        List<Task> currentTasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(String.valueOf(approverId))
                .active()
                .orderByTaskCreateTime()
                .asc()
                .list();
        if (currentTasks.isEmpty()) {
            return List.of();
        }
        List<String> processInstanceIds = currentTasks.stream()
                .map(Task::getProcessInstanceId)
                .toList();
        Map<String, Integer> taskOrder = new HashMap<>();
        for (int index = 0; index < processInstanceIds.size(); index++) {
            taskOrder.putIfAbsent(processInstanceIds.get(index), index);
        }
        return flowMapper.findPendingApplicationsByProcessInstanceIds(processInstanceIds).stream()
                .sorted(Comparator.comparingInt(application ->
                        taskOrder.getOrDefault(application.getProcessInstanceId(), Integer.MAX_VALUE)
                ))
                .map(FlowApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalTaskResponse> listDone(Long approverId) {
        return flowMapper.findCompletedTasksByApprover(approverId).stream()
                .map(ApprovalTaskResponse::from)
                .toList();
    }

    @Transactional
    public FlowApplicationResponse approve(
            Long applicationId,
            Long approverId,
            ApprovalRequest request
    ) {
        return process(applicationId, approverId, APPROVE, request.comment());
    }

    @Transactional
    public FlowApplicationResponse reject(
            Long applicationId,
            Long approverId,
            ApprovalRequest request
    ) {
        return process(applicationId, approverId, REJECT, request.comment());
    }

    private FlowApplicationResponse submit(
            Long applicantId,
            String applicationType,
            ApplicationRequest request
    ) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        Long approverId = employeeDirectory.findDirectLeaderId(applicantId);
        if (approverId == null) {
            throw new BusinessException(FlowErrorCode.DIRECT_LEADER_MISSING);
        }
        Long secondApproverId = null;
        if (LEAVE.equals(applicationType)) {
            secondApproverId = employeeDirectory.findDirectLeaderId(approverId);
            if (secondApproverId == null) {
                throw new BusinessException(FlowErrorCode.SECOND_APPROVER_MISSING);
            }
        }

        FlowApplication application = new FlowApplication();
        application.setApplicationNo(generateApplicationNo(applicationType));
        application.setApplicantId(applicantId);
        application.setApproverId(approverId);
        application.setApplicationType(applicationType);
        application.setStartTime(request.startTime());
        application.setEndTime(request.endTime());
        application.setReason(request.reason().trim());
        application.setStatus(PENDING);
        flowMapper.insertApplication(application);

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantId", String.valueOf(applicantId));
        variables.put("directLeaderId", String.valueOf(approverId));
        if (secondApproverId != null) {
            variables.put("secondApproverId", String.valueOf(secondApproverId));
        }
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                processDefinitionKey(applicationType),
                String.valueOf(application.getId()),
                variables
        ).getId();
        // Business rows and Flowable runtime tables share this local transaction.
        flowMapper.updateProcessInstanceId(application.getId(), processInstanceId);
        return FlowApplicationResponse.from(requireApplication(application.getId()));
    }

    private FlowApplicationResponse process(
            Long applicationId,
            Long approverId,
            String action,
            String comment
    ) {
        FlowApplication application = requireApplication(applicationId);
        if (!PENDING.equals(application.getStatus())) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        if (application.getApplicantId().equals(approverId)) {
            throw new BusinessException(FlowErrorCode.SELF_APPROVAL_FORBIDDEN);
        }
        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(application.getProcessInstanceId())
                .taskCandidateOrAssigned(String.valueOf(approverId))
                .singleResult();
        if (currentTask == null) {
            throw new BusinessException(FlowErrorCode.NOT_APPROVER);
        }

        boolean approved = APPROVE.equals(action);
        taskService.complete(currentTask.getId(), Map.of("approved", approved));
        flowMapper.insertApprovalRecord(
                applicationId,
                approverId,
                action,
                comment == null || comment.isBlank() ? null : comment.trim()
        );
        if (!approved) {
            updateStatus(applicationId, REJECTED);
            publishApprovalCompletedEventAfterCommit(application, REJECTED);
        } else if (isProcessEnded(application.getProcessInstanceId())) {
            updateStatus(applicationId, APPROVED);
            publishApprovalCompletedEventAfterCommit(application, APPROVED);
        } else {
            flowMapper.updateCurrentApproverIfPending(
                    applicationId,
                    findNextApproverId(application.getProcessInstanceId())
            );
        }
        return FlowApplicationResponse.from(requireApplication(applicationId));
    }

    private void publishApprovalCompletedEventAfterCommit(FlowApplication application, String result) {
        ApprovalCompletedEvent event = new ApprovalCompletedEvent(
                "approval-completed:" + application.getId() + ":" + result,
                application.getId(),
                application.getApplicationType(),
                application.getApplicantId(),
                result,
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishApprovalCompletedEvent(event);
                }
            });
            return;
        }
        publishApprovalCompletedEvent(event);
    }

    private void publishApprovalCompletedEvent(ApprovalCompletedEvent event) {
        try {
            approvalEventPublisher.publish(event);
        } catch (RuntimeException ex) {
            log.error("Failed to publish approval completed event, eventId={}", event.eventId(), ex);
        }
    }

    private void updateStatus(Long applicationId, String status) {
        if (flowMapper.updateApplicationStatusIfPending(applicationId, status) == 0) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
    }

    private boolean isProcessEnded(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult() == null;
    }

    private Long findNextApproverId(String processInstanceId) {
        Task nextTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();
        if (nextTask == null) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        return taskService.getIdentityLinksForTask(nextTask.getId()).stream()
                .filter(identityLink -> "candidate".equals(identityLink.getType()))
                .map(IdentityLink::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .map(Long::valueOf)
                .findFirst()
                .orElseThrow(() -> new BusinessException(FlowErrorCode.SECOND_APPROVER_MISSING));
    }

    private FlowApplication requireApplication(Long id) {
        FlowApplication application = flowMapper.findApplicationById(id);
        if (application == null) {
            throw new BusinessException(FlowErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private String generateApplicationNo(String applicationType) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return applicationType.substring(0, 1) + date + suffix;
    }

    private String processDefinitionKey(String applicationType) {
        return switch (applicationType) {
            case LEAVE -> LEAVE_APPROVAL_KEY;
            case OVERTIME -> OVERTIME_APPROVAL_KEY;
            default -> throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        };
    }
}
