package com.sealand.gateway.core.filter.flowCtl;

import com.sealand.common.config.Rule;

/**
 * @author cgh
 * @create 2024-01-11
 * @desc 限流接口
 */
public interface IGatewayFlowCtlRule {
    void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId);
}
