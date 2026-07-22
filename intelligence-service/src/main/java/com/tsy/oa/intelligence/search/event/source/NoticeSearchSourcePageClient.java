package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "notice-service",
        contextId = "noticeSearchSourcePageClient",
        url = "${oa.search.sources.notice-url:}",
        path = "/internal/notices/search-source"
)
public interface NoticeSearchSourcePageClient {

    @GetMapping
    ApiResponse<NoticeSearchSourcePageResponse> getPage(
            @RequestParam int page,
            @RequestParam int pageSize
    );

    record NoticeSearchSourcePageResponse(
            List<NoticeSearchSourceClient.NoticeSearchSourceResponse> items,
            long total,
            int page,
            int pageSize,
            boolean hasNext
    ) {
    }
}
