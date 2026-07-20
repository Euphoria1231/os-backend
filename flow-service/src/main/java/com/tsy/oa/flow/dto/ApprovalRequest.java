package com.tsy.oa.flow.dto;

import jakarta.validation.constraints.Size;

public record ApprovalRequest(
        @Size(max = 500, message = "审批意见不能超过500个字符") String comment
) {
}
