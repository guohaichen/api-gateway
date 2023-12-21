package com.sealand.gateway.config.center.zookeeper;

import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.config.center.api.RulesChangeListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author cgh
 * @create 2023-12-21
 * @desc zookeeper实现注册中心
 */
public class ZookeeperConfigCenter implements ConfigCenter {

    private String serverAddress;

    private String env;

    @Override
    public void init(String serverAddress, String env) {
        this.serverAddress = serverAddress;
        this.env = env;
        //创建配置
        CuratorFramework client = CuratorFrameworkFactory.newClient(this.serverAddress, new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener rulesChangeListener) {

    }
}
