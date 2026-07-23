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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    private static final String PUBLISHED = "PUBLISHED";

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
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
        return NoticeResponse.from(requirePublishedNotice(notice.getId()));
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
        return NoticeResponse.from(requirePublishedNotice(noticeId));
    }

    @Transactional
    public void delete(Long noticeId) {
        if (noticeMapper.softDeletePublished(noticeId) != 1) {
            throw new BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
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
}
