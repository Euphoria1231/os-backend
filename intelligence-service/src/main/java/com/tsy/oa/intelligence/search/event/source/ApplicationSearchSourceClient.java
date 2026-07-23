package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
        name = "flow-service",
        contextId = "applicationSearchSourceClient",
        path = "/internal/flow/search-source"
)
public interface ApplicationSearchSourceClient {

    @GetMapping("/{applicationId}")
    ApiResponse<ApplicationSearchSourceResponse> getById(@PathVariable long applicationId);

    record ApplicationSearchSourceResponse(
            Long id,
            Long applicantId,
            Long approverId,
            List<Long> approverIds,
            String applicationType,
            String status,
            String reason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long searchVersion
    ) {

        public ApplicationSearchSourceResponse(
                Long id,
                Long applicantId,
                Long approverId,
                String applicationType,
                String status,
                String reason,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
            this(
                    id, applicantId, approverId, List.of(approverId), applicationType,
                    status, reason, createdAt, updatedAt, 1L
            );
        }
    }
}
