package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisRequest;
import com.tsy.oa.intelligence.ai.service.AiAnalysisAuditResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalAnalysisService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private final ApplicationAnalysisSource source;
    private final AiAnalysisService aiAnalysisService;

    public ApprovalAnalysisService(ApplicationAnalysisSource source, AiAnalysisService aiAnalysisService) {
        this.source = source;
        this.aiAnalysisService = aiAnalysisService;
    }

    public ApprovalAnalysisResponse analyze(long applicationId, long employeeId, List<String> roles) {
        ApplicationSearchSourceClient.ApplicationSearchSourceResponse application = source.findById(applicationId);
        if (application == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND);
        }
        if (!hasRole(roles, SUPER_ADMIN) && employeeId != application.applicantId() && employeeId != application.approverId()) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        String summary = "申请类型：" + application.applicationType() + "；状态：" + application.status()
                + "；提交时间：" + application.createdAt();
        List<String> warnings = "PENDING".equalsIgnoreCase(application.status())
                ? List.of("申请尚未完成审批，请核对业务材料。") : List.of("请结合当前申请状态核对业务材料。");
        AiAnalysisAuditResult analysis = aiAnalysisService.analyzeAndRecord(new AiAnalysisRequest(
                "APPROVAL", String.valueOf(applicationId), employeeId,
                "仅分析申请类型、状态、时间和原因：" + application.applicationType() + "；" + application.status()
                        + "；" + application.createdAt() + "；" + application.reason()
        ));
        AiCallResult result = analysis.result();
        String advice = result.status().name().equals("SUCCESS") ? result.displayText()
                : "仅供参考：AI 暂不可用，请人工核对申请材料后按制度处理。";
        return new ApprovalAnalysisResponse(analysis.analysisId(), result.status(), applicationId, summary, warnings, advice, "仅供参考");
    }

    private boolean hasRole(List<String> roles, String role) {
        return roles != null && roles.stream().anyMatch(role::equals);
    }
}
