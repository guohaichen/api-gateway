package com.sealand.gateway.core.filter.loadbalance;

import com.sealand.common.config.DynamicConfigManager;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.enums.ResponseCode;
import com.sealand.common.exception.NotFoundException;
import com.sealand.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cgh
 * @create 2023-12-14
 * @desc 轮询负载均衡
 */
@Slf4j
public class RoundRobinLoadBalanceRule implements IGatewayLoadBalanceRule {
    //轮询下标
    private AtomicInteger roundPosition = new AtomicInteger(1);
    private final String serviceId;

    public RoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String, RoundRobinLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RoundRobinLoadBalanceRule getInstance(String serviceId) {
        RoundRobinLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RoundRobinLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance chooseServiceByContext(GatewayContext ctx) {
        return chooseServiceById(ctx.getUniqueId());
    }

    @Override
    public ServiceInstance chooseServiceById(String serviceId) {
        Set<ServiceInstance> serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId);
        if (serviceInstanceSet.isEmpty()) {
            log.warn("{} hasn't instance for providing;", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instancesList = new ArrayList<>(serviceInstanceSet);
        if (instancesList.isEmpty()) {
            log.warn("{} hasn't instance for providing", serviceId);
            return null;
        } else {
            int roundPosition = Math.abs(this.roundPosition.incrementAndGet());
            return instancesList.get(roundPosition % instancesList.size());
        }
    }
}
