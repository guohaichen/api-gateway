package com.sealand.gateway.core.filter.loadbalance;

import com.sealand.common.config.ServiceInstance;
import com.sealand.gateway.core.context.GatewayContext;

/**
 * @author cgh
 * @create 2023-12-14
 * @desc 负载均衡顶级接口
 */
public interface IGatewayLoadBalanceRule {
    /**
     * 通过上下文获取服务示例
     * @param gatewayContext
     * @return
     */
    ServiceInstance chooseServiceByContext(GatewayContext gatewayContext);

    /**
     * 通过服务id获取对应的服务实例
     * @param serviceId
     * @return
     */
    ServiceInstance chooseServiceById(String serviceId);
}
