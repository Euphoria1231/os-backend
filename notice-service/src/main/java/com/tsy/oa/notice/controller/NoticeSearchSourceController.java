package com.tsy.oa.notice.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.notice.dto.NoticeSearchSourcePageResponse;
import com.tsy.oa.notice.dto.NoticeSearchSourceResponse;
import com.tsy.oa.notice.service.NoticeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/notices/search-source")
public class NoticeSearchSourceController {

    private final NoticeService noticeService;

    public NoticeSearchSourceController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/{id}")
    public ApiResponse<NoticeSearchSourceResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(noticeService.getSearchSource(id));
    }

    @GetMapping
    public ApiResponse<NoticeSearchSourcePageResponse> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(noticeService.listSearchSource(page, pageSize));
    }
}
