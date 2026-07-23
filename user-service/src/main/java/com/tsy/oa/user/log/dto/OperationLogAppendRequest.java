package com.tsy.oa.user.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OperationLogAppendRequest(
        @Positive(message = "操作人员工ID必须为正数") Long operatorId,
        @Size(max = 100, message = "操作人名称不能超过100个字符") String operatorName,
        @NotBlank(message = "服务名称不能为空")
        @Size(max = 50, message = "服务名称不能超过50个字符")
        String serviceName,
        @NotBlank(message = "业务模块不能为空")
        @Size(max = 50, message = "业务模块不能超过50个字符")
        String businessModule,
        @NotBlank(message = "操作类型不能为空")
        @Size(max = 50, message = "操作类型不能超过50个字符")
        String operationType,
        @Size(max = 50, message = "目标业务类型不能超过50个字符") String targetType,
        @Size(max = 100, message = "目标业务ID不能超过100个字符") String targetId,
        @NotBlank(message = "操作摘要不能为空")
        @Size(max = 2000, message = "操作摘要不能超过2000个字符")
        String summary,
        @NotBlank(message = "操作结果不能为空")
        @Pattern(regexp = "SUCCESS|FAILURE", message = "操作结果只能是SUCCESS或FAILURE")
        String operationStatus,
        @NotBlank(message = "请求路径不能为空")
        @Size(max = 1000, message = "请求路径不能超过1000个字符")
        String requestPath,
        @NotBlank(message = "HTTP Method不能为空")
        @Size(max = 10, message = "HTTP Method不能超过10个字符")
        String httpMethod,
        @Size(max = 128, message = "客户端IP不能超过128个字符") String clientIp,
        @Size(max = 4000, message = "错误摘要不能超过4000个字符") String errorMessage
) {
}
