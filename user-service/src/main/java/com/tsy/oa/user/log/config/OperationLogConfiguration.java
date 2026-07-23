package com.tsy.oa.user.log.config;

import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.OperationLogCommand;
import com.tsy.oa.user.log.dto.OperationLogAppendRequest;
import com.tsy.oa.user.log.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperationLogConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogConfiguration.class);

    @Bean
    public BusinessOperationLogger userBusinessOperationLogger(OperationLogService operationLogService) {
        return new BusinessOperationLogger(
                "user-service",
                command -> operationLogService.append(toAppendRequest(command)),
                (command, exception) -> LOGGER.error(
                        "业务操作日志写入失败，模块={}，操作={}",
                        command.businessModule(),
                        command.operationType(),
                        exception
                )
        );
    }

    private OperationLogAppendRequest toAppendRequest(OperationLogCommand command) {
        return new OperationLogAppendRequest(
                command.operatorId(),
                command.operatorName(),
                command.serviceName(),
                command.businessModule(),
                command.operationType(),
                command.targetType(),
                command.targetId(),
                command.summary(),
                command.operationStatus(),
                command.requestPath(),
                command.httpMethod(),
                command.clientIp(),
                command.errorMessage()
        );
    }
}
