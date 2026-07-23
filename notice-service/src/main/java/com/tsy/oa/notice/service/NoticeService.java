package com.tsy.oa.notice.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.notice.dto.NoticePublishRequest;
import com.tsy.oa.notice.dto.NoticeResponse;
import com.tsy.oa.notice.dto.NoticeSearchSourcePageResponse;
import com.tsy.oa.notice.dto.NoticeSearchSourceResponse;
import com.tsy.oa.notice.error.NoticeErrorCode;
import com.tsy.oa.notice.mapper.NoticeMapper;
import com.tsy.oa.notice.model.Notice;
import com.tsy.oa.notice.model.NoticeView;
import com.tsy.oa.notice.search.SearchIndexEvent;
import com.tsy.oa.notice.search.SearchIndexEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    private static final String PUBLISHED = "PUBLISHED";

    private final NoticeMapper noticeMapper;
    private final SearchIndexEventPublisher searchIndexEventPublisher;

    public NoticeService(
            NoticeMapper noticeMapper,
            SearchIndexEventPublisher searchIndexEventPublisher
    ) {
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
        Notice publishedNotice = requirePublishedNotice(notice.getId());
        publishSearchIndexEvent(publishedNotice, SearchIndexEvent.Operation.UPSERT);
        return NoticeResponse.from(publishedNotice);
    }

    @Transactional
    public NoticeResponse update(Long noticeId, NoticePublishRequest request) {
        Notice notice = new Notice();
        notice.setId(noticeId);
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        if (noticeMapper.updatePublished(notice) != 1) {
            throw new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        Notice updatedNotice = requirePublishedNotice(noticeId);
        publishSearchIndexEvent(updatedNotice, SearchIndexEvent.Operation.UPSERT);
        return NoticeResponse.from(updatedNotice);
    }

    @Transactional
    public void delete(Long noticeId) {
        if (noticeMapper.softDeletePublished(noticeId) != 1) {
            throw new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        publishSearchIndexEvent(
                noticeId,
                noticeMapper.findSearchVersionById(noticeId),
                SearchIndexEvent.Operation.DELETE
        );
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
    public NoticeSearchSourceResponse getSearchSource(Long noticeId) {
        return NoticeSearchSourceResponse.from(requirePublishedNotice(noticeId));
    }

    @Transactional(readOnly = true)
    public NoticeSearchSourcePageResponse listSearchSource(int page, int pageSize) {
        int offset = checkedOffset(page, pageSize);
        long total = noticeMapper.countPublished();
        List<NoticeSearchSourceResponse> items = noticeMapper
                .findPublishedPage(offset, pageSize)
                .stream()
                .map(NoticeSearchSourceResponse::from)
                .toList();
        return new NoticeSearchSourcePageResponse(
                items, total, page, pageSize, (long) page * pageSize < total
        );
    }

    private int checkedOffset(int page, int pageSize) {
        long offset = Math.multiplyExact((long) page - 1L, pageSize);
        if (offset > Integer.MAX_VALUE) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return (int) offset;
    }

    private Notice requirePublishedNotice(Long id) {
        Notice notice = noticeMapper.findPublishedById(id);
        if (notice == null) {
            throw new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        return notice;
    }

    private void publishSearchIndexEvent(Notice notice, SearchIndexEvent.Operation operation) {
        publishSearchIndexEvent(notice.getId(), notice.getSearchVersion(), operation);
    }

    private void publishSearchIndexEvent(
            Long noticeId,
            Long searchVersion,
            SearchIndexEvent.Operation operation
    ) {
        if (noticeId == null || searchVersion == null) {
            throw new IllegalStateException("Notice search event version is unavailable");
        }
        searchIndexEventPublisher.publish(new SearchIndexEvent(
                "notice:" + noticeId + ":v:" + searchVersion,
                SearchIndexEvent.AggregateType.NOTICE,
                operation,
                noticeId,
                searchVersion,
                null
        ));
    }
}
