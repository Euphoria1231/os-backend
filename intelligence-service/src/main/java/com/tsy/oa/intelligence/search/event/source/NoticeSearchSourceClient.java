package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@FeignClient(
        name = "notice-service",
        contextId = "noticeSearchSourceClient",
        path = "/internal/notices/search-source"
)
public interface NoticeSearchSourceClient {

    @GetMapping("/{noticeId}")
    ApiResponse<NoticeSearchSourceResponse> getById(@PathVariable long noticeId);

    record NoticeSearchSourceResponse(
            Long id,
            String title,
            String content,
            String status,
            LocalDateTime publishedAt
    ) {
    }
}
