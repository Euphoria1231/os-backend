package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import feign.FeignException;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.springframework.stereotype.Component;

@Component
public class FeignApplicationAnalysisSource implements ApplicationAnalysisSource {
    private final ApplicationSearchSourceClient client;
    public FeignApplicationAnalysisSource(ApplicationSearchSourceClient client) { this.client = client; }
    @Override public ApplicationSearchSourceClient.ApplicationSearchSourceResponse findById(long applicationId) {
        try {
            ApiResponse<ApplicationSearchSourceClient.ApplicationSearchSourceResponse> response = client.getById(applicationId);
            if (response != null && response.code() == 0 && response.data() != null) {
                return response.data();
            }
            if (response != null && response.code() >= 40400 && response.code() < 40500) {
                return null;
            }
            throw new IllegalStateException("Application analysis source is unavailable");
        } catch (FeignException.NotFound exception) {
            return null;
        }
    }
}
