package com.sealand.gateway.register.center.etcd;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.BasicConst;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
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


    private final String separator = BasicConst.PATH_SEPARATOR;

    @Override
    public void init(String registerAddress, String env) {
        if (client == null) {
            synchronized (lock) {
                if (client == null) {
                    client = Client.builder().endpoints(registerAddress).build();
                }
            }
        }
        kvClient = client.getKVClient();
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        ByteSequence key = ByteSequence.from((REGISTER_CENTER_PREFIX + separator + serviceDefinition.getServiceId() + separator
                + serviceInstance.getIp() + BasicConst.COLON_SEPARATOR + serviceInstance.getPort()).getBytes());
        log.info("etcd put key :{}", key);
        ByteSequence value = ByteSequence.from(JSON.toJSONString(serviceInstance).getBytes());
        try {
            kvClient.put(key, value).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("etcd 创建服务示例失败：" + e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        ByteSequence key = ByteSequence.from((REGISTER_CENTER_PREFIX + separator + serviceDefinition.getServiceId() + separator
                + serviceInstance.getIp() + BasicConst.COLON_SEPARATOR + serviceInstance.getPort()).getBytes());
        kvClient.delete(key);
        log.info("{} 服务实例下线...", serviceDefinition.getServiceId() + separator + serviceInstance.getIp() + separator + serviceInstance.getPort());
    }

    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {
        /* 根据服务前缀key，找到所有的服务定义，找到所有的服务实例；
        监听所有的服务实例，如果服务实例发生变化，更新
         */
        //服务定义前缀
        ByteSequence prefixKey = ByteSequence.from((REGISTER_CENTER_PREFIX + separator).getBytes());
        //使用前缀查询
        GetOption getOption = GetOption.newBuilder().isPrefix(true).build();
        try {
            GetResponse response = kvClient.get(prefixKey, getOption).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


    }
}
