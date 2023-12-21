package com.sealand.gateway.register.center.zookeeper;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.gateway.register.center.api.RegisterCenter;
import com.sealand.gateway.register.center.api.RegisterCenterListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;

import java.util.HashMap;
import java.util.Map;

import static com.sealand.gateway.register.center.zookeeper.ZookeeperRegisterConstants.REGISTER_CENTER_ZOOKEEPER_PREFIX;


@Slf4j
public class ZookeeperRegisterCenter implements RegisterCenter {

    private String registerAddress;

    private String env;

    private CuratorFramework curatorClient;

    @Override
    public void init(String registerAddress, String env) {

        this.registerAddress = registerAddress;
        this.env = env;

        //Curator工厂类创建客户端对象
        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(registerAddress)
                .namespace(env)
                .build();
        //启动客户端
        curatorClient.start();
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            //创建服务信息创建节点
            curatorClient.create().creatingParentsIfNeeded().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + serviceDefinition.getServiceId(), JSON.toJSONBytes(serviceInstance));
            log.info("zookeeper 写入服务成功，服务信息:{}", JSON.toJSONString(serviceInstance));
        } catch (Exception e) {
            log.error("zookeeper 创建节点失败,错误信息:{}", e.getMessage());
            throw new RuntimeException("zookeeper 创建节点失败!");
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        //删除节点,guaranteed保证即使出现网络故障，也可以删除节点，deletingChildrenIfNeeded表级联删除
        try {
            curatorClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + serviceDefinition.getServiceId());
        } catch (Exception e) {
            log.error("zookeeper 删除节点失败，错误信息:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //todo
    @Override
    public void subscribeAllServicesChange(RegisterCenterListener registerCenterListener) {

    }
}
