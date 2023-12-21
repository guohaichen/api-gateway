package com.sealand.gateway.core.center;

import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.config.center.nacos.NacosConfigCenter;
import com.sealand.gateway.config.center.zookeeper.ZookeeperConfigCenter;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.nacos.NacosRegisterCenter;
import com.sealand.gateway.register.center.zookeeper.ZookeeperRegisterCenter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author cgh
 * @create 2023-12-21
 * @desc 根据配置 选择相应的配置中心和注册中心
 */
@Slf4j
public class ConfigAndRegisterCenterFactory {

    public static final String NACOS_CENTER = "nacos";
    public static final String ZOOKEEPER_CENTER = "zookeeper";

    /**
     * 根据配置选择注册中心
     * @param registerType 参数——类型
     * @return 注册中心
     */
    public static RegisterCenter chooseRegisterCenterType(String registerType) {
        if (NACOS_CENTER.equals(registerType)) {
            return new NacosRegisterCenter();
        } else if (ZOOKEEPER_CENTER.equals(registerType)) {
            return new ZookeeperRegisterCenter();
        } else {
            log.error("配置参数出错，暂时不支持该注册中心:{}配置;",registerType);
            throw new IllegalArgumentException("配置参数出错，暂时不支持该注册中心配置;");
        }
    }

    /**
     * 根据配置选择配置中心
     * @param configType 参数类型
     * @return 配置中心
     */
    public static ConfigCenter chooseConfigCenterType(String configType) {
        if (NACOS_CENTER.equals(configType)) {
            return new NacosConfigCenter();
        } else if (ZOOKEEPER_CENTER.equals(configType)) {
            return new ZookeeperConfigCenter();
        } else {
            log.error("配置参数出错，暂时不支持该配置中心:{}配置;",configType);
            throw new IllegalArgumentException("配置参数出错，暂时不支持该配置中心;");
        }
    }
}
