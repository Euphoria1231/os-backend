package com.tsy.oa.intelligence.search.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oa.search.elasticsearch")
public class ElasticsearchSearchProperties {

    @NotBlank
    private String url = "http://127.0.0.1:9200";

    @NotBlank
    private String noticeIndex = "oa-notices-v1";

    @NotBlank
    private String applicationIndex = "oa-applications-v1";

    @NotBlank
    private String noticeAlias = "oa-notices";

    @NotBlank
    private String applicationAlias = "oa-applications";

    private boolean initialize = true;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNoticeIndex() {
        return noticeIndex;
    }

    public void setNoticeIndex(String noticeIndex) {
        this.noticeIndex = noticeIndex;
    }

    public String getApplicationIndex() {
        return applicationIndex;
    }

    public void setApplicationIndex(String applicationIndex) {
        this.applicationIndex = applicationIndex;
    }

    public String getNoticeAlias() {
        return noticeAlias;
    }

    public void setNoticeAlias(String noticeAlias) {
        this.noticeAlias = noticeAlias;
    }

    public String getApplicationAlias() {
        return applicationAlias;
    }

    public void setApplicationAlias(String applicationAlias) {
        this.applicationAlias = applicationAlias;
    }

    public boolean isInitialize() {
        return initialize;
    }

    public void setInitialize(boolean initialize) {
        this.initialize = initialize;
    }
}
