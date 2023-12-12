package com.sealand.gateway.client.support;

import com.sealand.gateway.register.center.api.RegisterCenter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.gateway.client.core.ApiProperties;

import java.util.ServiceLoader;

/**
 * @author cgh
 * @create 2023-11-13
 * @desc
 */
@Slf4j
public abstract class AbstractClientRegisterManager {
    @Getter
    private ApiProperties apiProperties;

    private RegisterCenter registerCenter;

    protected AbstractClientRegisterManager(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;

        //初始化注册中心对象
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        for (RegisterCenter firstRegisterCenter : serviceLoader) {
            if (firstRegisterCenter != null) {
                registerCenter = firstRegisterCenter;
                break;
            }
        }
        if (registerCenter == null) {
            log.info("cannot found RegisterCenter impl");
            throw new RuntimeException("cannot found RegisterCenter impl");
        }
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());
    }

    protected void register(ServiceDefinition serviceDefinition, ServiceInstance instance) {
        registerCenter.register(serviceDefinition, instance);
    }
}
