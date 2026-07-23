package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.springframework.stereotype.Component;

@Component
public class FeignApplicationAnalysisSource implements ApplicationAnalysisSource {
    private final ApplicationSearchSourceClient client;
    public FeignApplicationAnalysisSource(ApplicationSearchSourceClient client) { this.client = client; }
    @Override public ApplicationSearchSourceClient.ApplicationSearchSourceResponse findById(long applicationId) {
        ApiResponse<ApplicationSearchSourceClient.ApplicationSearchSourceResponse> response = client.getById(applicationId);
        return response != null && response.code() == 0 ? response.data() : null;
    }
}
