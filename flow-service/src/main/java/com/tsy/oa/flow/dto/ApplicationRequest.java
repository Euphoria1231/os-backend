package com.tsy.oa.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ApplicationRequest(
        @NotNull(message = "开始时间不能为空") LocalDateTime startTime,
        @NotNull(message = "结束时间不能为空") LocalDateTime endTime,
        @NotBlank(message = "申请原因不能为空")
        @Size(max = 500, message = "申请原因不能超过500个字符")
        String reason
) {
}
