package com.sealand.gateway.core.filter.flowCtl;

import com.sealand.common.config.Rule;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.filter.Filter;
import com.sealand.gateway.core.filter.FilterAspect;

import java.util.Iterator;
import java.util.Set;

import static com.sealand.common.constants.FilterConst.*;

/**
 * @author cgh
 * @create 2024-01-11
 * @desc 流控过滤器
 */
@FilterAspect(id = FLOW_CTL_FILTER_ID, name = FLOW_CTL_FILTER_NAME, order = FLOW_CTL_FILTER_ORDER)
public class FlowCtlFilter implements Filter {
    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
        Rule rule = gatewayContext.getRule();
        if (rule != null) {
            Set<Rule.FlowCtlConfig> flowCtlConfigs = rule.getFlowCtlConfigs();
            Iterator<Rule.FlowCtlConfig> iterator = flowCtlConfigs.iterator();
            Rule.FlowCtlConfig flowCtlConfig;
            while (iterator.hasNext()) {
                IGatewayFlowCtlRule flowCtlRule = null;
                flowCtlConfig = iterator.next();
                if (flowCtlConfig == null) {
                    continue;
                }
                String path = gatewayContext.getRequest().getPath();
                if (flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_PATH) && path.equals(flowCtlConfig.getValue())) {
                    //通过http路径限流
                    flowCtlRule = FlowCtlRuleByPath.getInstance(rule.getServiceId(), path);
                } else if (flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_SERVICE)) {
                    //todo 通过服务限流
                }
                if (flowCtlRule != null) {
                    flowCtlRule.doFlowCtlFilter(flowCtlConfig, rule.getServiceId());
                }
            }
        }

    }
}
