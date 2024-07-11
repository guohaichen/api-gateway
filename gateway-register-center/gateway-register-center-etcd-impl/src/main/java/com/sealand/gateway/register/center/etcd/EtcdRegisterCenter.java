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
import io.etcd.jetcd.options.GetOption;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author cgh
 * @create 2024/7/4
 */
@Slf4j
public class EtcdRegisterCenter implements RegisterCenter {

    private volatile Client client;

    private String env;

    private KV kvClient;

    private final Object lock = new Object();

    private final String REGISTER_CENTER_PREFIX = "/api-gateway/service";

    //服务定义前缀
    private final String SERVICE_DEFINITION_PREFIX = "/api-gateway/service-definition";

    /**
     * 监听器列表
     */
    private final List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();


    private final String separator = BasicConst.PATH_SEPARATOR;

    @Override
    public void init(String registerAddress, String env) {
        this.env = env;
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

        //写入服务定义
        String sd = separator + env + SERVICE_DEFINITION_PREFIX + separator + serviceDefinition.getServiceId();
        ByteSequence serviceDefinitionKey = ByteSequence.from(sd.getBytes());
        ByteSequence serviceDefinitionValue = ByteSequence.from(JSON.toJSONBytes(serviceDefinition));
        try {
            //判断是否已经创建过服务实例
            if (kvClient.get(serviceDefinitionKey).get().getKvs().isEmpty()) {
                kvClient.put(serviceDefinitionKey, serviceDefinitionValue);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        //写入服务实例
        ByteSequence serviceInstanceKey = ByteSequence.from((separator + env + REGISTER_CENTER_PREFIX + separator + serviceDefinition.getServiceId() + separator
                + serviceInstance.getIp() + BasicConst.COLON_SEPARATOR + serviceInstance.getPort()).getBytes());
        log.info("etcd put serviceInstanceKey: {}", serviceInstanceKey);
        ByteSequence serviceInstanceValue = ByteSequence.from(JSON.toJSONBytes(serviceInstance));
        kvClient.put(serviceInstanceKey, serviceInstanceValue);

    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        ByteSequence key = ByteSequence.from((separator + env + REGISTER_CENTER_PREFIX + separator + serviceDefinition.getServiceId() + separator
                + serviceInstance.getIp() + BasicConst.COLON_SEPARATOR + serviceInstance.getPort()).getBytes());
        kvClient.delete(key);

        //serviceDefinitionList.remove(separator + env + REGISTER_CENTER_PREFIX + separator + serviceDefinition.getServiceId());
        log.info("{} 服务实例下线...", serviceDefinition.getServiceId() + separator + serviceInstance.getIp() + separator + serviceInstance.getPort());
    }

    //todo 缺少服务实例/定义变化的回调，更新DynamicConfigManager
    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {
        registerCenterListenerList.add(registerCenterListener);

        doSubscribeChange();
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

        scheduledThreadPool.scheduleWithFixedDelay(this::doSubscribeChange, 10, 10, TimeUnit.SECONDS);
    }

    private void doSubscribeChange() {
        List<ServiceDefinition> serviceDefinitionList = new ArrayList<>();

        //服务定义前缀
        ByteSequence prefixKey = ByteSequence.from((separator + env + SERVICE_DEFINITION_PREFIX + separator).getBytes());
        GetOption prefixOption = GetOption.newBuilder().isPrefix(true).build();
        try {
            kvClient.get(prefixKey, prefixOption).get().getKvs().forEach(keyValue -> {
                serviceDefinitionList.add(JSON.parseObject(keyValue.getValue().toString(), ServiceDefinition.class));
            });
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


        //找到所有的服务定义
        for (ServiceDefinition sd : serviceDefinitionList) {

            String key = separator + env + REGISTER_CENTER_PREFIX + separator + sd.getServiceId();
            log.info("key :{} ", key);
            ByteSequence watchKey = ByteSequence.from(key.getBytes());

            //todo 应该写监听器事件实现，目前硬编码
            Set<ServiceInstance> serviceInstanceSet = new HashSet<>();
            try {
                kvClient.get(watchKey, GetOption.newBuilder().isPrefix(true).build()).get().getKvs().forEach(
                        keyValue -> {
                            serviceInstanceSet.add(JSON.parseObject(keyValue.getValue().toString(), ServiceInstance.class));
                        }
                );
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            log.info("register listener onchange 回调...");
            registerCenterListenerList.forEach(l -> l.onChange(sd, serviceInstanceSet));

//            //实例化一个监听器对象，当指定key，注意 watch机制包含前缀监听  发生变化时被调用
//            client.getWatchClient().watch(watchKey, Watch.listener(watchResponse -> {
//                Set<ServiceInstance> instanceSet = new HashSet<>();
//                watchResponse.getEvents().forEach(
//                        watchEvent -> {
//                            if (WatchEvent.EventType.PUT.equals(watchEvent.getEventType())) {
//                                log.info("--------------watch listener----------");
//                                instanceSet.add(JSON.parseObject(watchEvent.getKeyValue().getValue().toString(), ServiceInstance.class));
//                            }
//                        }
//                );
//                log.info("onchange回调...");
//                registerCenterListenerList.forEach(l -> l.onChange(sd, instanceSet));
//            }));
        }
    }
}
