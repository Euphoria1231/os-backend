package com.tsy.oa.flow.log;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.OperationLogCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service",
        contextId = "flowOperationLogClient",
        path = "/internal/user/operation-logs"
)
public interface OperationLogClient {

    @PostMapping
    ApiResponse<Void> append(@RequestBody OperationLogCommand command);
}
