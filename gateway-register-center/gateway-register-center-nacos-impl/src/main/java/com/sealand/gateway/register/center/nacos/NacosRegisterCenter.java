package com.sealand.gateway.register.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.GatewayConst;
import com.sealand.common.utils.JSONUtil;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
/**
 * @author cgh
 * @create 2023-11-09
 * @desc
 */ public class NacosRegisterCenter implements RegisterCenter {
    /**
     * nacos 注册中心地址
     */
    private String registerAddress;

    /**
     * 环境
     */
    private String env;

    /**
     * 提供了注册实例、取消注册实例、获取指定服务实例，以及订阅指定服务以接收实例更改事件、取消订阅，关闭服务发现与注册等能力。
     */
    private NamingService namingService;

    /**
     * 直接操作Nacos服务器，提供了更新实例、查询服务、创建服务、删除服务、更新服务等能力。
     */
    private NamingMaintainService namingMaintainService;

    /**
     * 监听器列表
     */
    private final List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();


    @Override
    public void init(String registerAddress, String env) {
        this.registerAddress = registerAddress;
        this.env = env;
        try {
            //nacos sdk
            this.namingService = NacosFactory.createNamingService(registerAddress);
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            //构造nacos实例信息
            Instance nacosInstance = new Instance();
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setPort(serviceInstance.getPort());
            nacosInstance.setIp(serviceInstance.getIp());

            HashMap<String, String> instanceMap = new HashMap<>();
            //nacos服务元数据来源 例如下面
            //meta={"enable":true,"ip":"192.168.126.3","port":8088,"registerTime":1701227892022,"serviceInstanceId":"192.168.126.3:8088"}
            instanceMap.put(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceInstance));
            nacosInstance.setMetadata(instanceMap);

            //注册
            namingService.registerInstance(serviceDefinition.getServiceId(), env, nacosInstance);

            //更新服务定义
            HashMap<String, String> definitionMap = new HashMap<>();
            definitionMap.put(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition));

            namingMaintainService.updateService(serviceDefinition.getServiceId(), env, 0, definitionMap);
            log.info("register : {}, {}", serviceDefinition, serviceInstance);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            //nacos 注销实例sdk(删除实例)
            namingService.deregisterInstance(serviceDefinition.getServiceId(), serviceInstance.getAddress(), serviceInstance.getPort());
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {
        registerCenterListenerList.add(registerCenterListener);
        doSubscribeAllServiceChange();

        //新服务加入的话，使用定时任务检查
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1, new NameThreadFactory("SubscribeAllServicesChangeFactory"));
        scheduledThreadPool.scheduleWithFixedDelay(this::doSubscribeAllServiceChange, 10, 10, TimeUnit.SECONDS);
    }

    //订阅所有服务变更
    private void doSubscribeAllServiceChange() {
        try {
            //已订阅的服务
            Set<String> subscribeService = namingService.getSubscribeServices().stream().map(ServiceInfo::getName).collect(Collectors.toSet());
            log.info("nacos已订阅的服务:{}", Arrays.toString(subscribeService.toArray()));
            int pageNo = 1;
            int pageSize = 100;

            //分页从nacos拿到服务列表
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, env).getData();
            while (CollectionUtils.isNotEmpty(serviceList)) {
                for (String service : serviceList) {
                    if (subscribeService.contains(service)) {
                        continue;
                    }

                    //nacos事件监听器
                    EventListener eventListener = new NacosRegisterListener();
                    eventListener.onEvent(new NamingEvent(service, null));
                    namingService.subscribe(service, env, eventListener);
                    log.info("订阅subscribe:\t{},环境:\t{}", service, env);
                }
                serviceList = namingService.getServicesOfServer(++pageNo, pageSize, env).getData();

            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    public class NacosRegisterListener implements EventListener {

        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                NamingEvent namingEvent = (NamingEvent) event;
                String serviceName = namingEvent.getServiceName();

                try {
                    //获取服务定义信息
                    Service service = namingMaintainService.queryService(serviceName, env);
                    ServiceDefinition serviceDefinition = JSON.parseObject(service.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);

                    //获取服务实例信息
                    List<Instance> allInstances = namingService.getAllInstances(service.getName(), env);
                    Set<ServiceInstance> set = new HashSet<>();

                    for (Instance instance : allInstances) {
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        set.add(serviceInstance);
                    }

                    registerCenterListenerList.forEach(l -> l.onChange(serviceDefinition, set));
                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
