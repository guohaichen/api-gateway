package com.sealand.gateway.config.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.sealand.common.config.Rule;
import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.config.center.api.RulesChangeListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
public class NacosConfigCenter implements ConfigCenter {

    private static final String DATA_ID = "api-gateway";

    private String serverAddress;

    private String env;
    /**
     * 提供获取配置、发布配置、删除配置，添加监听器、删除监听器等能力。
     */
    private ConfigService configService;


    /**
     * 调用 Nacos sdk
     *
     * @param serverAddress
     * @param env
     */
    @Override
    public void init(String serverAddress, String env) {
        this.serverAddress = serverAddress;
        this.env = env;

        try {
            configService = NacosFactory.createConfigService(serverAddress);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener rulesChangeListener) {
        try {
            //初始化通知
            String config = configService.getConfig(DATA_ID, env, 5000);
            log.info("config from nacos: {}", config);
            List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
            rulesChangeListener.onRulesChange(rules);

            //监听变化
            configService.addListener(DATA_ID, env, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("config from nacos : {}", configInfo);

                    List<Rule> rules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
                    rulesChangeListener.onRulesChange(rules);
                }
            });

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }
}
