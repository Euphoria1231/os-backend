package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowApprovedLeaveProviderTests {

    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 21);

    @Test
    void rejectsErrorResponseFromFlowService() {
        FlowApprovedLeaveClient client = date -> ApiResponse.failure(50301, "审批服务错误");
        FlowApprovedLeaveProvider provider = new FlowApprovedLeaveProvider(client);

        assertThatThrownBy(() -> provider.findApprovedLeaves(WORK_DATE))
                .isInstanceOf(ApprovedLeaveUnavailableException.class);
    }

    @Test
    void returnsApprovedLeavesFromSuccessfulResponse() {
        ApprovedLeave leave = new ApprovedLeave(10L, WORK_DATE, WORK_DATE, "APPROVED");
        FlowApprovedLeaveClient client = date -> ApiResponse.success(List.of(leave));
        FlowApprovedLeaveProvider provider = new FlowApprovedLeaveProvider(client);

        assertThat(provider.findApprovedLeaves(WORK_DATE)).containsExactly(leave);
    }
}
