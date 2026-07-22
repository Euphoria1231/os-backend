package com.tsy.oa.notice.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.notice.dto.NoticePublishRequest;
import com.tsy.oa.notice.dto.NoticeResponse;
import com.tsy.oa.notice.dto.NoticeSearchSourceResponse;
import com.tsy.oa.notice.error.NoticeErrorCode;
import com.tsy.oa.notice.event.SearchIndexEvent;
import com.tsy.oa.notice.event.SearchIndexEventPublisher;
import com.tsy.oa.notice.mapper.NoticeMapper;
import com.tsy.oa.notice.model.Notice;
import com.tsy.oa.notice.model.NoticeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private static final String PUBLISHED = "PUBLISHED";
    private static final int MAX_SEARCH_PAGE_SIZE = 100;

    private final NoticeMapper noticeMapper;
    private final SearchIndexEventPublisher searchIndexEventPublisher;

    public NoticeService(NoticeMapper noticeMapper, SearchIndexEventPublisher searchIndexEventPublisher) {
        this.noticeMapper = noticeMapper;
        this.searchIndexEventPublisher = searchIndexEventPublisher;
    }

    @Transactional
    public NoticeResponse publish(Long publisherId, NoticePublishRequest request) {
        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setPublisherId(publisherId);
        notice.setStatus(PUBLISHED);
        notice.setPublishedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        publishSearchIndexEventAfterCommit(notice, PUBLISHED);
        return NoticeResponse.from(requirePublishedNotice(notice.getId()));
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> listForEmployee(Long employeeId) {
        return noticeMapper.findPublishedForEmployee(employeeId).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoticeResponse getForEmployee(Long noticeId, Long employeeId) {
        NoticeView notice = noticeMapper.findPublishedByIdForEmployee(noticeId, employeeId);
        if (notice == null) {
            throw new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void markRead(Long noticeId, Long employeeId) {
        requirePublishedNotice(noticeId);
        noticeMapper.insertReadIfAbsent(noticeId, employeeId);
    }

    @Transactional(readOnly = true)
    public int countUnread(Long employeeId) {
        return noticeMapper.countUnread(employeeId);
    }

    @Transactional(readOnly = true)
    public List<NoticeSearchSourceResponse> listSearchSource(int page, int pageSize) {
        int normalizedPageSize = normalizeSearchPageSize(pageSize);
        int offset = calculateOffset(page, normalizedPageSize);
        return noticeMapper.findSearchSource(offset, normalizedPageSize).stream()
                .map(NoticeSearchSourceResponse::from)
                .toList();
    }

    private Notice requirePublishedNotice(Long id) {
        Notice notice = noticeMapper.findPublishedById(id);
        if (notice == null) {
            throw new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }

    private void publishSearchIndexEventAfterCommit(Notice notice, String operation) {
        SearchIndexEvent event = new SearchIndexEvent(
                "search-index:notice:" + notice.getId() + ":" + operation,
                "NOTICE",
                notice.getId(),
                operation,
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSearchIndexEvent(event);
                }
            });
            return;
        }
        publishSearchIndexEvent(event);
    }

    private void publishSearchIndexEvent(SearchIndexEvent event) {
        try {
            searchIndexEventPublisher.publish(event);
        } catch (RuntimeException ex) {
            log.error("Failed to publish notice search index event, eventId={}", event.eventId(), ex);
        }
    }

    private int normalizeSearchPageSize(int pageSize) {
        if (pageSize < 1) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return Math.min(pageSize, MAX_SEARCH_PAGE_SIZE);
    }

    private int calculateOffset(int page, int pageSize) {
        if (page < 1) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return (page - 1) * pageSize;
    }
}
