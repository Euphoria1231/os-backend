package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;

public interface SearchDocumentSourceGateway {

    NoticeSearchDocument loadNotice(long noticeId);

    ApplicationSearchDocument loadApplication(long applicationId);
}
