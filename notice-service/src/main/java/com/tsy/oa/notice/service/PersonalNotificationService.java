package com.tsy.oa.notice.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.notice.dto.PersonalNotificationPageResponse;
import com.tsy.oa.notice.dto.PersonalNotificationResponse;
import com.tsy.oa.notice.error.NoticeErrorCode;
import com.tsy.oa.notice.mapper.PersonalNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonalNotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PersonalNotificationMapper notificationMapper;

    public PersonalNotificationService(PersonalNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public PersonalNotificationPageResponse list(Long employeeId, int page, int pageSize) {
        int offset = checkedOffset(page, pageSize);
        long total = notificationMapper.countByRecipient(employeeId);
        List<PersonalNotificationResponse> items = notificationMapper
                .findPageByRecipient(employeeId, offset, pageSize)
                .stream()
                .map(PersonalNotificationResponse::from)
                .toList();
        return new PersonalNotificationPageResponse(items, total, page, pageSize);
    }

    @Transactional(readOnly = true)
    public int countUnread(Long employeeId) {
        return notificationMapper.countUnread(employeeId);
    }

    @Transactional
    public void markRead(Long notificationId, Long employeeId) {
        if (!notificationMapper.existsByIdAndRecipient(notificationId, employeeId)) {
            throw new BusinessException(NoticeErrorCode.PERSONAL_NOTIFICATION_NOT_FOUND);
        }
        notificationMapper.markRead(notificationId, employeeId);
    }

    @Transactional
    public void markAllRead(Long employeeId) {
        notificationMapper.markAllRead(employeeId);
    }

    private int checkedOffset(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        long offset = Math.multiplyExact((long) page - 1L, pageSize);
        if (offset > Integer.MAX_VALUE) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return (int) offset;
    }
}
