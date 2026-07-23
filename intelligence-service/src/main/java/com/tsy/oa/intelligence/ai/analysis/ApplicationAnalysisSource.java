package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;

@FunctionalInterface
public interface ApplicationAnalysisSource {

    ApplicationSearchSourceClient.ApplicationSearchSourceResponse findById(long applicationId);
}
