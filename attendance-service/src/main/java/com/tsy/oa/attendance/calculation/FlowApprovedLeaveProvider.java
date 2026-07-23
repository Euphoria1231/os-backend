package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class FlowApprovedLeaveProvider implements ApprovedLeaveProvider {

    private final FlowApprovedLeaveClient client;

    public FlowApprovedLeaveProvider(FlowApprovedLeaveClient client) {
        this.client = client;
    }

    @Override
    public List<ApprovedLeave> findApprovedLeaves(LocalDate date) {
        ApiResponse<List<ApprovedLeave>> response;
        try {
            response = client.findApprovedLeaves(date.toString());
        } catch (RuntimeException exception) {
            throw new ApprovedLeaveUnavailableException("审批服务不可用", exception);
        }
        if (response == null || response.code() != 0 || response.data() == null) {
            throw new ApprovedLeaveUnavailableException("审批服务返回错误");
        }
        return List.copyOf(response.data());
    }
}
