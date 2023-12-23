package com.sealand.gateway.register.center.zookeeper;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.BasicConst;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.sealand.gateway.register.center.zookeeper.ZookeeperRegisterConstants.REGISTER_CENTER_ZOOKEEPER_PREFIX;


@Slf4j
public class ZookeeperRegisterCenter implements RegisterCenter {

    private String registerAddress;

    private String env;
    @Getter
    private CuratorFramework curatorClient;

    private final List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();

    @Override
    public void init(String registerAddress, String env) {

        this.registerAddress = registerAddress;
        this.env = env;

        //Curator工厂类创建客户端对象
        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(registerAddress)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(env)
                .build();
        //启动客户端
        curatorClient.start();
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            //创建服务信息创建节点
            byte[] bytes = curatorClient.getData().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId());
            if (bytes.length == 0) {
                curatorClient.create().creatingParentsIfNeeded().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId(), JSON.toJSONBytes(serviceInstance));
                log.info("zookeeper 写入服务成功，服务信息:{}", JSON.toJSONString(serviceInstance));
            }
        } catch (Exception e) {
            log.error("zookeeper 创建节点失败,错误信息:{}", e.getMessage());
            throw new RuntimeException("zookeeper 创建节点失败!");
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        //删除节点,guaranteed保证即使出现网络故障，也可以删除节点，deletingChildrenIfNeeded表级联删除
        try {
            curatorClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId());
        } catch (Exception e) {
            log.error("zookeeper 删除节点失败，错误信息:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //todo
    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {

        registerCenterListenerList.add(registerCenterListener);
        //todo
        try {
            Set<ServiceInstance> serviceInstanceSet = new HashSet<>();
            ServiceDefinition serviceDefinition = new ServiceDefinition();
            serviceDefinition.setUniqueId("api-gateway:1.0.0");
            registerCenterListenerList.forEach(l -> l.onChange(serviceDefinition, serviceInstanceSet));
            //curator查询已订阅的服务
            PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorClient, REGISTER_CENTER_ZOOKEEPER_PREFIX, true);


            pathChildrenCache.start(PathChildrenCache.StartMode.NORMAL);
            //监听事件
            pathChildrenCache.getListenable().addListener((curatorFramework, event) -> {
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
                    log.info("子节点更新");
                    log.info("节点:{}", event.getData().getPath());
                    String service = new String(event.getData().getData());
                    log.info("数据:{}", service);
                    serviceInstanceSet.add(JSON.parseObject(service, ServiceInstance.class));

                } else if (event.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
                    log.info("初始化操作");

                } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                    log.info("删除子节点");
                    log.info("节点:{}", event.getData().getPath());
                    String service = new String(event.getData().getData());
                    log.info("数据:{}", service);
                    serviceInstanceSet.remove(JSON.parseObject(service, ServiceInstance.class));

                } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                    log.info("添加子节点");
                    log.info("节点:{}", event.getData().getPath());
                    String service = new String(event.getData().getData());
                    log.info("数据:{}", service);
                    serviceInstanceSet.add(JSON.parseObject(service, ServiceInstance.class));
                }

            });
        } catch (
                Exception e) {
            throw new RuntimeException(e);
        }
    }
}
