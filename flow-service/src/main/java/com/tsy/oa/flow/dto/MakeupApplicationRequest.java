package com.tsy.oa.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MakeupApplicationRequest(
        @NotNull(message = "考勤记录ID不能为空")
        @Positive(message = "考勤记录ID必须为正数")
        Long attendanceRecordId,
        @NotBlank(message = "补签原因不能为空")
        @Size(max = 500, message = "补签原因不能超过500个字符")
        String reason
) {
}
