package com.tsy.oa.notice.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.notice.dto.NoticeSearchSourceResponse;
import com.tsy.oa.notice.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/notices")
public class InternalNoticeController {

    private final NoticeService noticeService;

    public InternalNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/search-source")
    public ApiResponse<List<NoticeSearchSourceResponse>> searchSource(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(noticeService.listSearchSource(page, pageSize));
    }
}
