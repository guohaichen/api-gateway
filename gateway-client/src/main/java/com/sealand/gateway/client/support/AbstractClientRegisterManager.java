package com.sealand.gateway.client.support;

import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.nacos.NacosRegisterCenter;
import com.sealand.gateway.register.center.zookeeper.ZookeeperRegisterCenter;
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

        String NACOS_REGISTER_CENTER = "nacos";
        String ZOOKEEPER_REGISTER_CENTER = "zookeeper";

        //todo 不灵活，写死了，
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        for (RegisterCenter registerCenter : serviceLoader) {

            if (registerCenter instanceof NacosRegisterCenter && NACOS_REGISTER_CENTER.equals(apiProperties.getRegisterType())) {
                this.registerCenter = registerCenter;
                log.info("nacos register init...");
            } else if (registerCenter instanceof ZookeeperRegisterCenter && ZOOKEEPER_REGISTER_CENTER.equals(apiProperties.getRegisterType())) {
                log.info("zookeeper register init...");
                this.registerCenter = registerCenter;
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
