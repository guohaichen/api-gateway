package com.sealand.gateway.core;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.DynamicConfigManager;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.utils.NetUtils;
import com.sealand.common.utils.TimeUtil;
import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.core.center.ConfigAndRegisterCenterFactory;
import com.sealand.gateway.core.config.Config;
import com.sealand.gateway.core.config.ConfigLoader;
import com.sealand.gateway.core.netty.Container;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Set;

import static com.sealand.common.constants.BasicConst.COLON_SEPARATOR;

/**
 * API网关启动类
 */
@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        //加载网关核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        //插件初始化

        //配置中心管理器初始化，连接配置中心，监听配置的新增、修改、删除
        configAndSubscribe(config);

        //启动容器，主要是netty服务
        Container container = new Container(config);
        container.start();

        //连接注册中心，将注册中心的实例加载到本地
        final RegisterCenter registerCenter = registerAndSubscribe(config);

        //服务优雅关机,收到kill信号时
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            registerCenter.deregister(buildGatewayServiceDefinition(config), buildGatewayServiceInstance(config));
            container.shutdown();
        }));
    }


    private static void configAndSubscribe(Config config) {
        //根据配置文件选择对应的注册中心
        final ConfigCenter configCenter = ConfigAndRegisterCenterFactory.chooseConfigCenterType(config.getRegisterAndConfigCenter());

        configCenter.init(config.getRegistryAddress(), config.getEnv());
        configCenter.subscribeRulesChange(ruleList -> DynamicConfigManager.getInstance().putAllRule(ruleList));
    }

    private static RegisterCenter registerAndSubscribe(Config config) {
        RegisterCenter registerCenter = ConfigAndRegisterCenterFactory.chooseRegisterCenterType(config.getRegisterAndConfigCenter());
        registerCenter.init(config.getRegistryAddress(), config.getEnv());

        //构造网关服务定义和服务实例（主要用于 nacos 注册中心注册服务和定义服务）
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);
        //注册
        registerCenter.register(serviceDefinition, serviceInstance);

        //订阅
        registerCenter.subscribeAllServicesChange(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
                //todo 
                if (serviceDefinition != null) {
                    log.info("refresh service and instance: {} {}", serviceDefinition.getUniqueId(),
                            JSON.toJSON(serviceInstanceSet));
                    DynamicConfigManager manager = DynamicConfigManager.getInstance();
                    manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);
                    manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
                }
            }
        });
        return registerCenter;

    }

    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId(localIp + COLON_SEPARATOR + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }

    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setInvokerMap(new HashMap<>());
        serviceDefinition.setUniqueId(config.getApplicationName());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setEnvType(config.getEnv());
        return serviceDefinition;
    }

}
