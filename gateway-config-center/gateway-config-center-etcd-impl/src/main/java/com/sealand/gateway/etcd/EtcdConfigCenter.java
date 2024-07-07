package com.sealand.gateway.etcd;

import com.sealand.gateway.config.center.api.ConfigCenter;
import com.sealand.gateway.config.center.api.RulesChangeListener;
import io.etcd.jetcd.Client;

/**
 * @author cgh
 * @create 2024/7/6
 * etcd 配置中心实现类
 */
public class EtcdConfigCenter implements ConfigCenter {


    private Client client;


    @Override
    public void init(String serverAddress, String env) {
        client = Client.builder().endpoints(serverAddress).build();
    }


    //todo
    @Override
    public void subscribeRulesChange(RulesChangeListener rulesChangeListener) {

    }
}
