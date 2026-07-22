package com.tsy.oa.flow.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.flow.dto.ApplicationRequest;
import com.tsy.oa.flow.dto.ApplicationSearchSourcePageResponse;
import com.tsy.oa.flow.dto.ApplicationSearchSourceResponse;
import com.tsy.oa.flow.dto.ApprovalRequest;
import com.tsy.oa.flow.dto.ApprovalTaskResponse;
import com.tsy.oa.flow.dto.FlowApplicationResponse;
import com.tsy.oa.flow.employee.EmployeeDirectory;
import com.tsy.oa.flow.error.FlowErrorCode;
import com.tsy.oa.flow.mapper.FlowMapper;
import com.tsy.oa.flow.model.FlowApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class FlowService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final FlowMapper flowMapper;
    private final EmployeeDirectory employeeDirectory;

    public FlowService(FlowMapper flowMapper, EmployeeDirectory employeeDirectory) {
        this.flowMapper = flowMapper;
        this.employeeDirectory = employeeDirectory;
    }

    @Transactional
    public FlowApplicationResponse submitLeave(Long applicantId, ApplicationRequest request) {
        return submit(applicantId, "LEAVE", request);
    }

    @Transactional
    public FlowApplicationResponse submitOvertime(Long applicantId, ApplicationRequest request) {
        return submit(applicantId, "OVERTIME", request);
    }

    @Transactional(readOnly = true)
    public List<FlowApplicationResponse> listMyApplications(Long applicantId) {
        return flowMapper.findApplicationsByApplicant(applicantId).stream()
                .map(FlowApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlowApplicationResponse> listTodo(Long approverId) {
        return flowMapper.findPendingApplicationsByApprover(approverId).stream()
                .map(FlowApplicationResponse::from)
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
        Long approverId = employeeDirectory.findDirectLeaderId(applicantId);
        if (approverId == null) {
            throw new BusinessException(FlowErrorCode.DIRECT_LEADER_MISSING);
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
        return FlowApplicationResponse.from(requireApplication(application.getId()));
    }

    private FlowApplicationResponse process(
            Long applicationId,
            Long approverId,
            String action,
            String targetStatus,
            String comment
    ) {
        FlowApplication application = requireApplication(applicationId);
        if (!application.getApproverId().equals(approverId)) {
            throw new BusinessException(FlowErrorCode.NOT_APPROVER);
        }
        if (!PENDING.equals(application.getStatus())) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        if (flowMapper.updateApplicationStatusIfPending(applicationId, targetStatus) == 0) {
            throw new BusinessException(FlowErrorCode.ALREADY_PROCESSED);
        }
        flowMapper.insertApprovalRecord(
                applicationId,
                approverId,
                action,
                comment == null || comment.isBlank() ? null : comment.trim()
        );
        return FlowApplicationResponse.from(requireApplication(applicationId));
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
