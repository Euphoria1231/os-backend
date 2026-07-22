package com.tsy.oa.intelligence.search.repository;

import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;

import java.io.IOException;
import java.util.List;

public interface SearchIndexRepository {

    void saveNotice(NoticeSearchDocument document) throws IOException;

    void deleteNotice(long noticeId) throws IOException;

    void saveApplication(ApplicationSearchDocument document) throws IOException;

    void deleteApplication(long applicationId) throws IOException;

    default void saveNotices(List<NoticeSearchDocument> documents) throws IOException {
        for (NoticeSearchDocument document : documents) {
            saveNotice(document);
        }
    }

    default void saveApplications(List<ApplicationSearchDocument> documents) throws IOException {
        for (ApplicationSearchDocument document : documents) {
            saveApplication(document);
        }
    }
}
