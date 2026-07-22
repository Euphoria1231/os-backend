package com.tsy.oa.intelligence.search.repository;

import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;

import java.io.IOException;

public interface SearchIndexRepository {

    void saveNotice(NoticeSearchDocument document) throws IOException;

    void deleteNotice(long noticeId) throws IOException;

    void saveApplication(ApplicationSearchDocument document) throws IOException;

    void deleteApplication(long applicationId) throws IOException;
}
