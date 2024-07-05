package com.sealand.gateway.etcd;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.BasicConst;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * @author cgh
 * @create 2024/7/4
 */
@Slf4j
public class EtcdRegisterCenter implements RegisterCenter {

    private volatile Client client;

    private KV kvClient;

    private final Object lock = new Object();

    private final String REGISTER_CENTER_PREFIX = "/api-gateway/service";

    private final List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();

    @Override
    public void init(String registerAddress, String env) {
        if (client == null) {
            synchronized (lock) {
                if (client == null) {
                    client = Client.builder().target(registerAddress).build();
                }
            }
        }
        kvClient = client.getKVClient();
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        ByteSequence key = ByteSequence.from((REGISTER_CENTER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId() + BasicConst.PATH_SEPARATOR
                + serviceInstance.getIp() + BasicConst.PATH_SEPARATOR + serviceInstance.getPort()).getBytes());
        ByteSequence value = ByteSequence.from(JSON.toJSONString(serviceInstance).getBytes());
        try {
            kvClient.put(key, value).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("etcd 创建服务示例失败：" + e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        //todo
    }

    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {
        registerCenterListenerList.add(registerCenterListener);


        Watch watchClient = client.getWatchClient();


    }
}
