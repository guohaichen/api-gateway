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
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;

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

    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {
        // notice 详细看看这里监听器链表回调的模式
        registerCenterListenerList.add(registerCenterListener);
        doOnChange();
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

        scheduledThreadPool.scheduleWithFixedDelay(this::doOnChange, 10, 10, TimeUnit.SECONDS);
    }

    /*
    监听服务实例前缀。服务实例是以 /env/api-gateway/service开头；服务定义是以 /env/api-gateway/service-definition开头
    反序列化成服务实例，getUniqueId,得到服务定义名称，找到服务定义 反序列化成服务定义，触发onchange
     */
    private void doOnChange() {
        String serviceInstancePrefixKey = separator + env + REGISTER_CENTER_PREFIX + separator;
        ByteSequence instanceKey = ByteSequence.from(serviceInstancePrefixKey.getBytes());
        WatchOption prefixOption = WatchOption.newBuilder().isPrefix(true).build();
        client.getWatchClient().watch(instanceKey, prefixOption, Watch.listener(
                watchResponse -> {
                    watchResponse.getEvents().forEach(watchEvent -> {
                        try {
                            if (WatchEvent.EventType.PUT.equals(watchEvent.getEventType())) {

                                ServiceInstance changeServiceInstance = JSON.parseObject(watchEvent.getKeyValue().getValue().toString(), ServiceInstance.class);
                                // uniqueId 是由 服务定义名称+版本号组成，这里通过subString去掉版本号，就是服务定义名称；
                                String serviceDefinitionName = changeServiceInstance.getUniqueId().substring(0,changeServiceInstance.getUniqueId().indexOf(':'));

                                ByteSequence serviceDefinitionKey = ByteSequence.from((separator + env + SERVICE_DEFINITION_PREFIX + separator + serviceDefinitionName).getBytes());
                                //根据服务定义名称找到服务定义并反序列化为服务定义；
                                ServiceDefinition serviceDefinition = JSON.parseObject(kvClient.get(serviceDefinitionKey).get().getKvs().get(0).getValue().toString(), ServiceDefinition.class);

                                ByteSequence instancePrefixKey = ByteSequence.from((serviceInstancePrefixKey + serviceDefinitionName).getBytes());
                                GetOption option = GetOption.newBuilder().isPrefix(true).build();
                                Set<ServiceInstance> set = new HashSet<>();
                                kvClient.get(instancePrefixKey, option).get().getKvs().forEach(
                                        keyValue -> {
                                            ServiceInstance serviceInstance = JSON.parseObject(keyValue.getValue().toString(), ServiceInstance.class);
                                            set.add(serviceInstance);
                                        }
                                );
                                log.info("serviceInstance change , instance :{}", changeServiceInstance.getServiceInstanceId());
                                registerCenterListenerList.forEach(l -> l.onChange(serviceDefinition, set));
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
        ));
    }
}
