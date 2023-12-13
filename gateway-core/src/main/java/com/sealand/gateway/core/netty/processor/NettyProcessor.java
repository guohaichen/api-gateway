package com.sealand.gateway.core.netty.processor;

import com.sealand.gateway.core.context.HttpRequestWrapper;

public interface NettyProcessor {
    void process(HttpRequestWrapper wrapper);
}
