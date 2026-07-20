package com.tsy.oa.flow.model;

import java.time.LocalDateTime;

public class ApprovalTaskRecord {

    private Long applicationId;
    private String applicationNo;
    private Long applicantId;
    private String applicationType;
    private String action;
    private String comment;
    private LocalDateTime processedAt;

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
