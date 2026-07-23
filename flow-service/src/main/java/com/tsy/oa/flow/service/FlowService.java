package com.tsy.oa.flow.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.common.notification.PersonalNotificationEvent;
import com.tsy.oa.flow.attendance.AttendanceMakeupGateway;
import com.tsy.oa.flow.attendance.AttendanceMakeupGateway.MakeupEligibility;
import com.tsy.oa.flow.dto.ApplicationRequest;
import com.tsy.oa.flow.dto.ApplicationSearchSourcePageResponse;
import com.tsy.oa.flow.dto.ApplicationSearchSourceResponse;
import com.tsy.oa.flow.dto.ApprovalProgressResponse;
import com.tsy.oa.flow.dto.ApprovalRequest;
import com.tsy.oa.flow.dto.ApprovalTaskResponse;
import com.tsy.oa.flow.dto.FlowApplicationResponse;
import com.tsy.oa.flow.dto.MakeupApplicationRequest;
import com.tsy.oa.flow.employee.EmployeeDirectory;
import com.tsy.oa.flow.employee.EmployeeDirectory.ApprovalRoute;
import com.tsy.oa.flow.error.FlowErrorCode;
import com.tsy.oa.flow.mapper.FlowMapper;
import com.tsy.oa.flow.mapper.MakeupFlowMapper;
import com.tsy.oa.flow.model.FlowApplication;
import com.tsy.oa.flow.model.ApprovalTaskRecord;
import com.tsy.oa.flow.notification.PersonalNotificationPublisher;
import com.tsy.oa.flow.search.SearchIndexEvent;
import com.tsy.oa.flow.search.SearchIndexEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class FlowService {

    private static final String PENDING = "PENDING";
    private static final String WAITING = "WAITING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final FlowMapper flowMapper;
    private final MakeupFlowMapper makeupFlowMapper;
    private final EmployeeDirectory employeeDirectory;
    private final AttendanceMakeupGateway attendanceMakeupGateway;
    private final PersonalNotificationPublisher notificationPublisher;
    private final SearchIndexEventPublisher searchIndexEventPublisher;

    public FlowService(
            FlowMapper flowMapper,
            MakeupFlowMapper makeupFlowMapper,
            EmployeeDirectory employeeDirectory,
            AttendanceMakeupGateway attendanceMakeupGateway,
            PersonalNotificationPublisher notificationPublisher,
            SearchIndexEventPublisher searchIndexEventPublisher
    ) {
        this.flowMapper = flowMapper;
        this.makeupFlowMapper = makeupFlowMapper;
        this.employeeDirectory = employeeDirectory;
        this.attendanceMakeupGateway = attendanceMakeupGateway;
        this.notificationPublisher = notificationPublisher;
        this.searchIndexEventPublisher = searchIndexEventPublisher;
    }

    @Transactional
    public FlowApplicationResponse submitLeave(Long applicantId, ApplicationRequest request) {
        return submit(applicantId, "LEAVE", request);
    }

    @Transactional
    public FlowApplicationResponse submitOvertime(Long applicantId, ApplicationRequest request) {
        return submit(applicantId, "OVERTIME", request);
    }

    @Transactional
    public FlowApplicationResponse submitMakeup(
            Long applicantId,
            MakeupApplicationRequest request
    ) {
        Long attendanceRecordId = request.attendanceRecordId();
        ensureNoActiveMakeupApplication(attendanceRecordId);
        MakeupEligibility eligibility = attendanceMakeupGateway.getEligibility(
                attendanceRecordId, applicantId
        );
        ApprovalRoute approvalRoute = requireApprovalRoute(applicantId);

        FlowApplication application = new FlowApplication();
        application.setApplicationNo(generateApplicationNo("MAKEUP"));
        application.setApplicantId(applicantId);
        application.setApproverId(approvalRoute.directLeaderId());
        application.setApplicationType("MAKEUP");
        application.setAttendanceRecordId(eligibility.attendanceRecordId());
        application.setMakeupActiveMarker(1);
        application.setReason(request.reason().trim());
        application.setStatus(PENDING);
        try {
            flowMapper.insertApplication(application);
        } catch (DuplicateKeyException exception) {
            if (hasActiveMakeupApplication(attendanceRecordId)) {
                throw new BusinessException(FlowErrorCode.DUPLICATE_MAKEUP_APPLICATION);
            }
            throw exception;
        }
        insertApprovalTasks(application.getId(), approvalRoute);
        publishApprovalTask(application, 1, approvalRoute.directLeaderId());
        FlowApplication persistedApplication = requireApplication(application.getId());
        publishSearchIndexEvent(persistedApplication);
        return toResponse(persistedApplication);
    }

    @Transactional(readOnly = true)
    public List<FlowApplicationResponse> listMyApplications(Long applicantId) {
        return flowMapper.findApplicationsByApplicant(applicantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlowApplicationResponse> listTodo(Long approverId) {
        return flowMapper.findPendingApplicationsByApprover(approverId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalTaskResponse> listDone(Long approverId) {
        return flowMapper.findCompletedTasksByApprover(approverId).stream()
                .map(ApprovalTaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationSearchSourceResponse getSearchSource(Long applicationId) {
        return ApplicationSearchSourceResponse.from(requireApplication(applicationId));
    }

    @Transactional(readOnly = true)
    public ApplicationSearchSourcePageResponse listSearchSource(int page, int pageSize) {
        int offset = checkedOffset(page, pageSize);
        long total = flowMapper.countApplications();
        List<ApplicationSearchSourceResponse> items = flowMapper
                .findApplicationPage(offset, pageSize)
                .stream()
                .map(ApplicationSearchSourceResponse::from)
                .toList();
        return new ApplicationSearchSourcePageResponse(
                items, total, page, pageSize, (long) page * pageSize < total
        );
    }

    private int checkedOffset(int page, int pageSize) {
        long offset = Math.multiplyExact((long) page - 1L, pageSize);
        if (offset > Integer.MAX_VALUE) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return (int) offset;
    }

    @Transactional
    public FlowApplicationResponse approve(
            Long applicationId,
            Long approverId,
            ApprovalRequest request
    ) {
        return process(applicationId, approverId, "APPROVE", APPROVED, request.comment());
    }

    @Transactional
    public FlowApplicationResponse reject(
            Long applicationId,
            Long approverId,
            ApprovalRequest request
    ) {
        return process(applicationId, approverId, "REJECT", REJECTED, request.comment());
    }

    private FlowApplicationResponse submit(
            Long applicantId,
            String applicationType,
            ApplicationRequest request
    ) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        ApprovalRoute approvalRoute = requireApprovalRoute(applicantId);

        FlowApplication application = new FlowApplication();
        application.setApplicationNo(generateApplicationNo(applicationType));
        application.setApplicantId(applicantId);
        application.setApproverId(approvalRoute.directLeaderId());
        application.setApplicationType(applicationType);
        application.setStartTime(request.startTime());
        application.setEndTime(request.endTime());
        application.setReason(request.reason().trim());
        application.setStatus(PENDING);
        flowMapper.insertApplication(application);
        insertApprovalTasks(application.getId(), approvalRoute);
        publishApprovalTask(application, 1, approvalRoute.directLeaderId());
        FlowApplication persistedApplication = requireApplication(application.getId());
        publishSearchIndexEvent(persistedApplication);
        return toResponse(persistedApplication);
    }

    private FlowApplicationResponse process(
            Long applicationId,
            Long approverId,
            String action,
            String targetStatus,
        String comment
    ) {
        FlowApplication application = requireApplication(applicationId);
        if (!PENDING.equals(application.getStatus())) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        ApprovalTaskRecord currentTask = flowMapper.findPendingTaskByApplication(applicationId);
        if (currentTask == null) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        if (!currentTask.getApproverId().equals(approverId)) {
            ApprovalTaskRecord employeeTask = flowMapper.findTaskByApplicationAndApprover(
                    applicationId, approverId
            );
            if (employeeTask != null
                    && (APPROVED.equals(employeeTask.getStatus())
                    || REJECTED.equals(employeeTask.getStatus()))) {
                throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
            }
            throw new BusinessException(FlowErrorCode.NOT_APPROVER);
        }
        if (flowMapper.updateApprovalTaskStatusIfPending(
                currentTask.getId(), targetStatus
        ) == 0) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        flowMapper.insertApprovalRecord(
                applicationId,
                currentTask.getId(),
                currentTask.getApprovalLevel(),
                approverId,
                action,
                comment == null || comment.isBlank() ? null : comment.trim()
        );

        boolean makeupApplication = "MAKEUP".equals(application.getApplicationType());
        if (REJECTED.equals(targetStatus)) {
            finishRejectedApplication(applicationId, makeupApplication);
            publishRejected(application, currentTask.getApprovalLevel());
        } else {
            ApprovalTaskRecord nextTask = flowMapper.findNextWaitingTask(
                    applicationId, currentTask.getApprovalLevel()
            );
            if (nextTask == null) {
                finishApprovedApplication(application);
                publishApproved(application);
            } else {
                activateNextTask(applicationId, nextTask);
                publishApprovalTask(
                        application,
                        nextTask.getApprovalLevel(),
                        nextTask.getApproverId()
                );
            }
        }
        FlowApplication updatedApplication = requireApplication(applicationId);
        publishSearchIndexEvent(updatedApplication);
        return toResponse(updatedApplication);
    }

    private void finishRejectedApplication(Long applicationId, boolean makeupApplication) {
        if (flowMapper.updateApplicationStatusIfPending(applicationId, REJECTED) == 0) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        flowMapper.cancelWaitingTasks(applicationId);
        if (makeupApplication) {
            makeupFlowMapper.releaseActiveMarker(applicationId);
        }
    }

    private void finishApprovedApplication(FlowApplication application) {
        if ("MAKEUP".equals(application.getApplicationType())) {
            attendanceMakeupGateway.completeMakeup(
                    application.getAttendanceRecordId(),
                    application.getApplicantId(),
                    application.getId()
            );
        }
        if (flowMapper.updateApplicationStatusIfPending(application.getId(), APPROVED) == 0) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
    }

    private void activateNextTask(Long applicationId, ApprovalTaskRecord nextTask) {
        if (flowMapper.activateApprovalTaskIfWaiting(nextTask.getId()) == 0
                || flowMapper.updateApplicationApproverIfPending(
                        applicationId, nextTask.getApproverId()
                ) == 0) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
    }

    private ApprovalRoute requireApprovalRoute(Long applicantId) {
        ApprovalRoute approvalRoute = employeeDirectory.findApprovalRoute(applicantId);
        if (approvalRoute == null || approvalRoute.directLeaderId() == null) {
            throw new BusinessException(FlowErrorCode.DIRECT_LEADER_MISSING);
        }
        if (approvalRoute.departmentLeaderId() == null) {
            throw new BusinessException(FlowErrorCode.DEPARTMENT_LEADER_MISSING);
        }
        return approvalRoute;
    }

    private void insertApprovalTasks(Long applicationId, ApprovalRoute approvalRoute) {
        flowMapper.insertApprovalTask(
                applicationId, 1, approvalRoute.directLeaderId(),
                approvalRoute.directLeaderName(), PENDING, LocalDateTime.now()
        );
        if (!approvalRoute.directLeaderId().equals(approvalRoute.departmentLeaderId())) {
            flowMapper.insertApprovalTask(
                    applicationId, 2, approvalRoute.departmentLeaderId(),
                    approvalRoute.departmentLeaderName(), WAITING, null
            );
        }
    }

    private void publishApprovalTask(
            FlowApplication application,
            int approvalLevel,
            Long approverId
    ) {
        notificationPublisher.publish(new PersonalNotificationEvent(
                "flow:" + application.getId() + ":task:" + approvalLevel,
                approverId,
                PersonalNotificationEvent.NotificationType.APPROVAL_TASK,
                "新的审批待办",
                applicationTypeLabel(application.getApplicationType())
                        + "申请 " + application.getApplicationNo() + " 等待您审批",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                application.getId(),
                LocalDateTime.now()
        ));
    }

    private void publishRejected(FlowApplication application, int approvalLevel) {
        notificationPublisher.publish(new PersonalNotificationEvent(
                "flow:" + application.getId() + ":rejected:" + approvalLevel,
                application.getApplicantId(),
                PersonalNotificationEvent.NotificationType.APPLICATION_REJECTED,
                "申请已被驳回",
                applicationTypeLabel(application.getApplicationType())
                        + "申请 " + application.getApplicationNo() + " 已被驳回",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                application.getId(),
                LocalDateTime.now()
        ));
    }

    private void publishApproved(FlowApplication application) {
        notificationPublisher.publish(new PersonalNotificationEvent(
                "flow:" + application.getId() + ":approved",
                application.getApplicantId(),
                PersonalNotificationEvent.NotificationType.APPLICATION_APPROVED,
                "申请已批准",
                applicationTypeLabel(application.getApplicationType())
                        + "申请 " + application.getApplicationNo() + " 已通过全部审批",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                application.getId(),
                LocalDateTime.now()
        ));
    }

    private String applicationTypeLabel(String applicationType) {
        return switch (applicationType) {
            case "LEAVE" -> "请假";
            case "OVERTIME" -> "加班";
            case "MAKEUP" -> "补签";
            default -> "办公";
        };
    }

    private void publishSearchIndexEvent(FlowApplication application) {
        if (application.getId() == null || application.getSearchVersion() == null) {
            throw new IllegalStateException("Application search event version is unavailable");
        }
        searchIndexEventPublisher.publish(new SearchIndexEvent(
                "application:" + application.getId() + ":v:" + application.getSearchVersion(),
                SearchIndexEvent.AggregateType.APPLICATION,
                SearchIndexEvent.Operation.UPSERT,
                application.getId(),
                application.getSearchVersion(),
                null
        ));
    }

    private FlowApplicationResponse toResponse(FlowApplication application) {
        List<ApprovalProgressResponse> approvalProgress = flowMapper
                .findTasksByApplication(application.getId())
                .stream()
                .map(ApprovalProgressResponse::from)
                .toList();
        return FlowApplicationResponse.from(application, approvalProgress);
    }

    private void ensureNoActiveMakeupApplication(Long attendanceRecordId) {
        if (hasActiveMakeupApplication(attendanceRecordId)) {
            throw new BusinessException(FlowErrorCode.DUPLICATE_MAKEUP_APPLICATION);
        }
    }

    private boolean hasActiveMakeupApplication(Long attendanceRecordId) {
        return makeupFlowMapper.findActiveApplicationIdByAttendanceRecordId(
                attendanceRecordId
        ) != null;
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
}
