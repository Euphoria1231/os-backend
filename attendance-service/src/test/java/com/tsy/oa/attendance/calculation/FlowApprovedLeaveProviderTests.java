package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    @Test
    void sendsApprovedLeaveDateInIsoFormat() {
        FlowApprovedLeaveClient client = mock(FlowApprovedLeaveClient.class);
        FlowApprovedLeaveProvider provider = new FlowApprovedLeaveProvider(client);

        when(client.findApprovedLeaves("2026-07-21"))
                .thenReturn(ApiResponse.success(List.of()));

        List<ApprovedLeave> result =
                provider.findApprovedLeaves(LocalDate.of(2026, 7, 21));

        assertThat(result).isEmpty();
        verify(client).findApprovedLeaves("2026-07-21");
    }
}
