package com.sealand.gateway.config.center.api;

/**
 * 配置中心接口，定义
 * 注册配置
 * 订阅变更
 */
public interface ConfigCenter {

    void init(String serverAddress, String env);

    void subscribeRulesChange(RulesChangeListener rulesChangeListener);
}
