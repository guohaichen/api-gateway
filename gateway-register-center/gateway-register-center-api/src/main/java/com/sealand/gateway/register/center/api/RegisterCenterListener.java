package com.sealand.gateway.register.center.api;

import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;

import java.util.Set;

/**
 * @author cgh
 * @create 2023-11-09
 * @desc 监听器
 */
public interface RegisterCenterListener {
    void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet);
}
