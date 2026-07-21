package com.tsy.oa.notice.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.notice.dto.NoticePublishRequest;
import com.tsy.oa.notice.dto.NoticeResponse;
import com.tsy.oa.notice.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping
    public ApiResponse<NoticeResponse> publish(
            @RequestHeader(EMPLOYEE_HEADER) Long publisherId,
            @Valid @RequestBody NoticePublishRequest request
    ) {
        return ApiResponse.success(noticeService.publish(publisherId, request));
    }

    @GetMapping
    public ApiResponse<List<NoticeResponse>> list(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(noticeService.listForEmployee(employeeId));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoticeResponse> getById(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(noticeService.getForEmployee(id, employeeId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        noticeService.markRead(id, employeeId);
        return ApiResponse.success(null);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(noticeService.countUnread(employeeId));
    }
}
