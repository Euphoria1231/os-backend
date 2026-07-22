package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "flow-service",
        contextId = "applicationSearchSourcePageClient",
        url = "${oa.search.sources.application-url:}",
        path = "/internal/flow/search-source"
)
public interface ApplicationSearchSourcePageClient {

    @GetMapping
    ApiResponse<ApplicationSearchSourcePageResponse> getPage(
            @RequestParam int page,
            @RequestParam int pageSize
    );

    record ApplicationSearchSourcePageResponse(
            List<ApplicationSearchSourceClient.ApplicationSearchSourceResponse> items,
            long total,
            int page,
            int pageSize,
            boolean hasNext
    ) {
    }
}
