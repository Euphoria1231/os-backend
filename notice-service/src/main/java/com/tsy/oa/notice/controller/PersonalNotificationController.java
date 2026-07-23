package com.tsy.oa.notice.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.notice.dto.PersonalNotificationPageResponse;
import com.tsy.oa.notice.service.PersonalNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices/personal")
public class PersonalNotificationController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final PersonalNotificationService notificationService;

    public PersonalNotificationController(PersonalNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PersonalNotificationPageResponse> list(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(notificationService.list(employeeId, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(notificationService.countUnread(employeeId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        notificationService.markRead(id, employeeId);
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        notificationService.markAllRead(employeeId);
        return ApiResponse.success(null);
    }
}
