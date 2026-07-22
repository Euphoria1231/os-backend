package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.intelligence.search.event.SearchDocumentNormalizer;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import org.springframework.stereotype.Component;

@Component
public class FeignSearchDocumentSourceGateway implements SearchDocumentSourceGateway {

    private final NoticeSearchSourceClient noticeClient;
    private final ApplicationSearchSourceClient applicationClient;
    private final SearchDocumentNormalizer documentNormalizer;

    public FeignSearchDocumentSourceGateway(
            NoticeSearchSourceClient noticeClient,
            ApplicationSearchSourceClient applicationClient,
            SearchDocumentNormalizer documentNormalizer
    ) {
        this.noticeClient = noticeClient;
        this.applicationClient = applicationClient;
        this.documentNormalizer = documentNormalizer;
    }

    @Override
    public NoticeSearchDocument loadNotice(long noticeId) {
        ApiResponse<NoticeSearchSourceClient.NoticeSearchSourceResponse> response = noticeClient.getById(noticeId);
        NoticeSearchSourceClient.NoticeSearchSourceResponse source = requireData(response, "notice", noticeId);
        return documentNormalizer.normalizeNotice(new NoticeSearchDocument(
                requireMatchingId(source.id(), noticeId, "notice"),
                source.title(),
                source.content(),
                source.publishedAt(),
                source.status()
        ));
    }

    @Override
    public ApplicationSearchDocument loadApplication(long applicationId) {
        ApiResponse<ApplicationSearchSourceClient.ApplicationSearchSourceResponse> response =
                applicationClient.getById(applicationId);
        ApplicationSearchSourceClient.ApplicationSearchSourceResponse source =
                requireData(response, "application", applicationId);
        return documentNormalizer.normalizeApplication(new ApplicationSearchDocument(
                requireMatchingId(source.id(), applicationId, "application"),
                requireId(source.applicantId(), "application applicant"),
                requireId(source.approverId(), "application approver"),
                source.applicationType(),
                source.status(),
                source.reason(),
                source.createdAt(),
                source.updatedAt()
        ));
    }

    private <T> T requireData(ApiResponse<T> response, String sourceType, long sourceId) {
        if (response == null || response.code() != 0 || response.data() == null) {
            throw new IllegalStateException(
                    "Unable to load " + sourceType + " search source for id [" + sourceId + "]"
            );
        }
        return response.data();
    }

    private long requireMatchingId(Long actualId, long expectedId, String sourceType) {
        long resolvedId = requireId(actualId, sourceType);
        if (resolvedId != expectedId) {
            throw new IllegalStateException(sourceType + " search source returned a mismatched id");
        }
        return resolvedId;
    }

    private long requireId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(fieldName + " id is missing");
        }
        return value;
    }

}
