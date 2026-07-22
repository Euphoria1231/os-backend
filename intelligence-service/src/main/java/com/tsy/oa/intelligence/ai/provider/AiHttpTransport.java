package com.tsy.oa.intelligence.ai.provider;

import java.io.IOException;

@FunctionalInterface
interface AiHttpTransport {

    AiHttpResponse exchange(AiHttpRequest request) throws IOException, InterruptedException;
}
