package com.sealand.gateway.register.center.api;


import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;

/**
 * @author cgh
 * @create 2023-11-09
 * @desc 注册中心接口
 */
public interface RegisterCenter {
    void init(String registerAddress, String env);

    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 订阅所有服务变更
     * @param registerCenterListener
     */
    void subscribeAllServicesChange(RegisterCenterListener registerCenterListener);

}

