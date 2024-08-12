package com.sealand.gateway.client.support;

import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.gateway.client.core.config.ApiProperties;
import com.sealand.gateway.register.center.etcd.EtcdRegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.nacos.NacosRegisterCenter;
import com.sealand.gateway.register.center.zookeeper.ZookeeperRegisterCenter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * @author cgh
 * @create 2023-11-13
 * @desc spi机制实现多注册中心实现，根据客户端的配置，支持nacos，etcd，zookeeper 注册中心
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
        String ETCD_REGISTER_CENTER = "etcd";

        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        for (RegisterCenter registerCenter : serviceLoader) {
            if (registerCenter instanceof NacosRegisterCenter && NACOS_REGISTER_CENTER.equals(apiProperties.getRegisterType())) {
                this.registerCenter = registerCenter;
                log.info("nacos register init...");
            } else if (registerCenter instanceof ZookeeperRegisterCenter && ZOOKEEPER_REGISTER_CENTER.equals(apiProperties.getRegisterType())) {
                log.info("zookeeper register init...");
                this.registerCenter = registerCenter;
            }else if (registerCenter instanceof EtcdRegisterCenter && ETCD_REGISTER_CENTER.equals(apiProperties.getRegisterType())){
                log.info("etcd register init...");
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
