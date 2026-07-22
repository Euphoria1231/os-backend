package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;

import java.util.Optional;

public interface SearchDocumentSourceGateway {

    NoticeSearchDocument loadNotice(long noticeId);

    ApplicationSearchDocument loadApplication(long applicationId);

    default Optional<NoticeSearchDocument> findNotice(long noticeId) {
        return Optional.ofNullable(loadNotice(noticeId));
    }

    default Optional<ApplicationSearchDocument> findApplication(long applicationId) {
        return Optional.ofNullable(loadApplication(applicationId));
    }
}
