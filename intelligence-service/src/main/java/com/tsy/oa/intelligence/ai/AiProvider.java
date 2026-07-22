package com.tsy.oa.intelligence.ai;

import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiPrompt;

public interface AiProvider {

    AiCallResult generate(AiPrompt prompt);
}
