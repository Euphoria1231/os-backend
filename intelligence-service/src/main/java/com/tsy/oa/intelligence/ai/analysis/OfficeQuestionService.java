package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisRequest;
import com.tsy.oa.intelligence.ai.service.AiAnalysisAuditResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import org.springframework.stereotype.Service;

@Service
public class OfficeQuestionService {
    private static final String POLICY = "考勤制度：迟到、缺卡应及时核对并按流程补正。审批制度：申请由申请人提交，审批人按权限处理。系统制度：账号仅限本人使用。";
    private final AiAnalysisService aiAnalysisService;
    public OfficeQuestionService(AiAnalysisService aiAnalysisService) { this.aiAnalysisService = aiAnalysisService; }
    public OfficeQuestionResponse ask(long employeeId, OfficeQuestionRequest request) {
        AiAnalysisAuditResult analysis = aiAnalysisService.analyzeAndRecord(new AiAnalysisRequest("OFFICE_QA", String.valueOf(employeeId), employeeId,
                "仅依据以下固定制度回答：" + POLICY + " 问题：" + request.question()));
        AiCallResult result = analysis.result();
        String answer = result.status().name().equals("SUCCESS") ? result.displayText() : fallback(request.question());
        return new OfficeQuestionResponse(analysis.analysisId(), result.status(), answer, "仅供参考");
    }
    private String fallback(String question) {
        if (question.contains("打卡") || question.contains("考勤")) return "仅供参考：" + POLICY.substring(0, POLICY.indexOf("审批制度"));
        if (question.contains("审批") || question.contains("申请")) return "仅供参考：审批制度：申请由申请人提交，审批人按权限处理。";
        return "仅供参考：" + POLICY;
    }
}
