package com.sealand.gateway.config.center.zookeeper;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.Rule;
import com.sealand.common.constants.BasicConst;
import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.config.center.api.RulesChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.List;

import static com.sealand.gateway.config.center.zookeeper.ZookeeperConfigConstants.Config_CENTER_ZOOKEEPER_PREFIX;

/**
 * @author cgh
 * @create 2023-12-21
 * @desc zookeeper实现注册中心
 */
@Slf4j
public class ZookeeperConfigCenter implements ConfigCenter {

    private static final String DATA_ID = "api-gateway";

    private static final String CONFIG_ID = Config_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + DATA_ID;

    private String serverAddress;

    private String env;

    private CuratorFramework client;

    @Override
    public void init(String serverAddress, String env) {
        this.serverAddress = serverAddress;
        this.env = env;
        //创建配置 //配置实例： /dev/api-gateway
        this.client = CuratorFrameworkFactory.builder()
                .connectString(this.serverAddress)
                .namespace(this.env)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener rulesChangeListener) {
        try {
            //获取配置
            byte[] bytesConfig = client.getData().forPath(CONFIG_ID);
            String config = new String(bytesConfig);
            List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);

            rulesChangeListener.onRulesChange(rules);

            //监听一个节点
            NodeCache nodeCache = new NodeCache(client, CONFIG_ID);
            //注册监听
            nodeCache.getListenable().addListener(new NodeCacheListener() {
                @Override
                public void nodeChanged() throws Exception {
                    if (nodeCache.getCurrentData() != null) {
                        log.info("zookeeper配置:{} 发生变化", CONFIG_ID);
                        String changedConfig = new String(nodeCache.getCurrentData().getData());
                        List<Rule> rules = JSON.parseObject(changedConfig).getJSONArray("rules").toJavaList(Rule.class);
                        rulesChangeListener.onRulesChange(rules);
                    }
                }
            });
        } catch (Exception e) {
            log.debug("zookeeper未获取到该配置:{}", CONFIG_ID);
            throw new RuntimeException(e);
        }

    }
}
