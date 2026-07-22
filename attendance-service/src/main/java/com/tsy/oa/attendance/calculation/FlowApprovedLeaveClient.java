package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "flow-service", path = "/internal/flow")
public interface FlowApprovedLeaveClient {

    @GetMapping("/approved-leaves")
    ApiResponse<List<ApprovedLeave>> findApprovedLeaves(@RequestParam("date") LocalDate date);
}
