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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author cgh
 * @create 2023-12-14
 * @desc 随机负载均衡
 */
@Slf4j
public class RandomLoadBalanceRule implements IGatewayLoadBalanceRule {

    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    public RandomLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static final ConcurrentHashMap<String, RandomLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RandomLoadBalanceRule getInstance(String serviceId) {
        RandomLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RandomLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance chooseServiceByContext(GatewayContext ctx) {
        String serviceId = ctx.getUniqueId();
        return chooseServiceById(serviceId);
    }

    @Override
    public ServiceInstance chooseServiceById(String serviceId) {
        Set<ServiceInstance> serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId);
        if (serviceInstanceSet.isEmpty()) {
            log.warn("{} hasn't instance for providing;", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instanceList = new ArrayList<>(serviceInstanceSet);
        //随机instance
        int index = ThreadLocalRandom.current().nextInt(instanceList.size());
        return instanceList.get(index);
    }
}
