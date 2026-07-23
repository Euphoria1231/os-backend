package com.tsy.oa.notice.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.notice.dto.NoticePublishRequest;
import com.tsy.oa.notice.dto.NoticeResponse;
import com.tsy.oa.notice.service.NoticeService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final BusinessOperationLogger operationLogger;

    public NoticeController(
            NoticeService noticeService,
            BusinessOperationLogger operationLogger
    ) {
        this.noticeService = noticeService;
        this.operationLogger = operationLogger;
    }

    @PostMapping
    public ApiResponse<NoticeResponse> publish(
            @RequestHeader(EMPLOYEE_HEADER) Long publisherId,
            @Valid @RequestBody NoticePublishRequest request,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, publisherId, "PUBLISH_NOTICE", null,
                "发布公告：" + request.title()
        );
        NoticeResponse response = operationLogger.execute(
                context,
                () -> noticeService.publish(publisherId, request),
                result -> result.id().toString()
        );
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<NoticeResponse> update(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long operatorId,
            @Valid @RequestBody NoticePublishRequest request,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "UPDATE_NOTICE", id,
                "修改公告：" + request.title()
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> noticeService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "DELETE_NOTICE", id, "删除公告"
        );
        operationLogger.execute(context, () -> noticeService.delete(id));
        return ApiResponse.success(null);
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

    private OperationLogContext logContext(
            HttpServletRequest request,
            Long operatorId,
            String operationType,
            Long noticeId,
            String summary
    ) {
        return HttpOperationLogContexts.create(
                request,
                operatorId,
                null,
                "NOTICE",
                operationType,
                "NOTICE",
                noticeId == null ? null : noticeId.toString(),
                summary
        );
    }
}
