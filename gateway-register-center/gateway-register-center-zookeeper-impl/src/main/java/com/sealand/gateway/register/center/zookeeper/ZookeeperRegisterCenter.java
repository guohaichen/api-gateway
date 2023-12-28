package com.sealand.gateway.register.center.zookeeper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.BasicConst;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ZookeeperRegisterCenter implements RegisterCenter {

    final static String REGISTER_CENTER_ZOOKEEPER_PREFIX = "/api-gateway/service";

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

    //todo 这里 ServiceDefinition 也应该写入到zookeeper
    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            String serviceDefinitionPath = REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId();
            //检查服务实例创建
            if (curatorClient.checkExists().forPath(serviceDefinitionPath) == null) {
                curatorClient.create().forPath(serviceDefinitionPath, JSON.toJSONBytes(serviceDefinition));
            }
            //创建服务信息创建节点
            String node = REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId() + BasicConst.PATH_SEPARATOR +
                    serviceInstance.getIp() + BasicConst.COLON_SEPARATOR + serviceInstance.getPort();
            if (curatorClient.checkExists().forPath(node) == null) {
                curatorClient.create().creatingParentsIfNeeded().forPath(node, JSON.toJSONBytes(serviceInstance));
                log.info("zookeeper 写入服务成功，服务信息:{}", JSON.toJSONString(serviceInstance));
            }
        } catch (Exception e) {
            log.error("zookeeper 创建节点失败,错误信息:{}", e.getMessage());
            throw new RuntimeException("zookeeper 创建节点失败!");
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        //删除节点,guaranteed保证即使出现网络故障，也可以删除节点 todo 停止项目时，发现没有执行删除，联合Bootstrap中 Runtime.getRuntime().addShutdownHook排查一下
        try {
            String node = REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinition.getServiceId() + BasicConst.PATH_SEPARATOR +
                    serviceInstance.getIp() + BasicConst.COLON_SEPARATOR + serviceInstance.getPort();
            curatorClient.delete().forPath(node);
        } catch (Exception e) {
            log.error("zookeeper 删除节点失败，错误信息:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //todo
    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {

        try {
            registerCenterListenerList.add(registerCenterListener);
            //查询所有服务
            List<String> serviceDefinitionList = curatorClient.getChildren().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX);
            if (CollectionUtils.isNotEmpty(serviceDefinitionList)) {

                for (String serviceDefinitionPath : serviceDefinitionList) {
                    Set<ServiceInstance> instanceHashSet = new HashSet<>();
                    //在zookeeper中获取serviceDefinition的值，并反序列化
                    byte[] bytes = curatorClient.getData().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinitionPath);
                    ServiceDefinition serviceDefinition = JSON.parseObject(new String(bytes), ServiceDefinition.class);

                    //根据ServiceDefinition查询所有的ServiceInstance;
                    List<String> serviceInstanceList = curatorClient.getChildren().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinitionPath);
                    if (CollectionUtils.isNotEmpty(serviceInstanceList)) {
                        for (String serviceInstancePath : serviceInstanceList) {
                            // api-gateway/service/服务定义/服务实例
                            String node = REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + serviceDefinitionPath + BasicConst.PATH_SEPARATOR + serviceInstancePath;
                            String serviceInstanceString = new String(curatorClient.getData().forPath(node));
                            ServiceInstance serviceInstance = JSON.parseObject(serviceInstanceString, ServiceInstance.class);
                            instanceHashSet.add(serviceInstance);
                        }
                        if (serviceDefinition != null) {
                            //首次扫描，将所有的服务建立关系，放在缓存DynamicConfigManager中；
                            registerCenterListener.onChange(serviceDefinition, instanceHashSet);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        watchNode();
    }

    //todo 事件监听仍需要进行onchange事件，
    private void watchNode() {
        /*//监听 ServiceDefinition
        PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorClient, REGISTER_CENTER_ZOOKEEPER_PREFIX, true);
        pathChildrenCache.getListenable().addListener((curatorFramework, event) -> {
            PathChildrenCacheEvent.Type type = event.getType();
            switch (type) {
                case CHILD_ADDED:

                    break;
                case CHILD_UPDATED:
                    break;
                case CHILD_REMOVED:
                    break;
            }
        });*/
        //使用curator.treeCache对各级子节点进行监听
        try {
            TreeCache treeCache = new TreeCache(curatorClient, REGISTER_CENTER_ZOOKEEPER_PREFIX);
            treeCache.getListenable().addListener((curatorFramework, treeCacheEvent) -> {
                String service;
                switch (treeCacheEvent.getType()) {
                    case NODE_ADDED:
                        log.info("添加节点:{}", treeCacheEvent.getData().getPath());
                        service = new String(treeCacheEvent.getData().getData());
                        log.info("数据:{}", service);
                        break;
                    case NODE_UPDATED:
                        log.info("更新节点:{}", treeCacheEvent.getData().getPath());
                        service = new String(treeCacheEvent.getData().getData());
                        log.info("数据:{}", service);
                        break;
                    case NODE_REMOVED:
                        log.info("删除节点:{}", treeCacheEvent.getData().getPath());
                        service = new String(treeCacheEvent.getData().getData());
                        log.info("数据:{}", service);
                        break;
                }
            });
            treeCache.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
