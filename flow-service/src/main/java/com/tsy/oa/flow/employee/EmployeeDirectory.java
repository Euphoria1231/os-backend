package com.tsy.oa.flow.employee;

public interface EmployeeDirectory {

    ApprovalRoute findApprovalRoute(Long employeeId);

    record ApprovalRoute(
            Long applicantId,
            Long directLeaderId,
            String directLeaderName,
            Long departmentLeaderId,
            String departmentLeaderName
    ) {
    }
}
