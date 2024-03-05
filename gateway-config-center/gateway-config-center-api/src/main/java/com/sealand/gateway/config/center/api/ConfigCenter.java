package com.sealand.gateway.config.center.api;

/**
 * 配置中心接口，定义
 * 注册配置
 * 订阅变更
 */
public interface ConfigCenter {
    /**
     * 网关核心配置文件的名称
     */
    String CONFIG_FILE_NAME = "api-gateway";

    void init(String serverAddress, String env);

    void subscribeRulesChange(RulesChangeListener rulesChangeListener);
}
