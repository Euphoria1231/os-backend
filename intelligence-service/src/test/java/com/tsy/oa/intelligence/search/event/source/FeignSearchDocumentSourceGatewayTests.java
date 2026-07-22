package com.tsy.oa.intelligence.search.event.source;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.intelligence.search.event.SearchDocumentNormalizer;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeignSearchDocumentSourceGatewayTests {

    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 7, 22, 8, 30);

    @Test
    void mapsSuccessfulNoticeResponse() {
        NoticeSearchSourceClient noticeClient = noticeId -> ApiResponse.success(
                new NoticeSearchSourceClient.NoticeSearchSourceResponse(
                        noticeId,
                        " 公司公告 ",
                        " 公告正文 ",
                        "PUBLISHED",
                        EVENT_TIME
                )
        );
        FeignSearchDocumentSourceGateway gateway = gateway(noticeClient, unusedApplicationClient());

        NoticeSearchDocument document = gateway.loadNotice(42L);

        assertThat(document.noticeId()).isEqualTo(42L);
        assertThat(document.title()).isEqualTo("公司公告");
        assertThat(document.content()).isEqualTo("公告正文");
    }

    @Test
    void rejectsFailedNoticeResponse() {
        NoticeSearchSourceClient noticeClient = noticeId -> ApiResponse.failure(40401, "公告不存在");
        FeignSearchDocumentSourceGateway gateway = gateway(noticeClient, unusedApplicationClient());

        assertThatThrownBy(() -> gateway.loadNotice(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("notice")
                .hasMessageContaining("42");
    }

    @Test
    void rejectsNullNoticeResponse() {
        NoticeSearchSourceClient noticeClient = noticeId -> null;
        FeignSearchDocumentSourceGateway gateway = gateway(noticeClient, unusedApplicationClient());

        assertThatThrownBy(() -> gateway.loadNotice(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("notice");
    }

    @Test
    void normalizesApplicationResponseAndRejectsMismatchedId() {
        ApplicationSearchSourceClient applicationClient = applicationId -> ApiResponse.success(
                new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                        applicationId,
                        3L,
                        2L,
                        "LEAVE",
                        "APPROVED",
                        "病".repeat(600),
                        EVENT_TIME,
                        EVENT_TIME.plusHours(1)
                )
        );
        FeignSearchDocumentSourceGateway gateway = gateway(unusedNoticeClient(), applicationClient);

        ApplicationSearchDocument document = gateway.loadApplication(15L);

        assertThat(document.reasonSummary()).hasSize(500);
        assertThat(document.approverId()).isEqualTo(2L);

        ApplicationSearchSourceClient mismatchedClient = applicationId -> ApiResponse.success(
                new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                        16L,
                        3L,
                        2L,
                        "LEAVE",
                        "APPROVED",
                        "申请原因",
                        EVENT_TIME,
                        EVENT_TIME
                )
        );
        FeignSearchDocumentSourceGateway mismatchedGateway = gateway(unusedNoticeClient(), mismatchedClient);
        assertThatThrownBy(() -> mismatchedGateway.loadApplication(15L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mismatched id");
    }

    private FeignSearchDocumentSourceGateway gateway(
            NoticeSearchSourceClient noticeClient,
            ApplicationSearchSourceClient applicationClient
    ) {
        return new FeignSearchDocumentSourceGateway(
                noticeClient,
                applicationClient,
                new SearchDocumentNormalizer()
        );
    }

    private NoticeSearchSourceClient unusedNoticeClient() {
        return noticeId -> ApiResponse.failure(50000, "unused");
    }

    private ApplicationSearchSourceClient unusedApplicationClient() {
        return applicationId -> ApiResponse.failure(50000, "unused");
    }
}
