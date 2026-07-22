package com.tsy.oa.notice.message.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.notice.message.dto.MessageResponse;
import com.tsy.oa.notice.message.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices/messages")
public class MessageController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<MessageResponse>> list(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(messageService.listMessages(employeeId, read, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(messageService.countUnread(employeeId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MessageResponse> getById(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(messageService.getMessage(id, employeeId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        messageService.markRead(id, employeeId);
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        messageService.markAllRead(employeeId);
        return ApiResponse.success(null);
    }
}
