package com.sealand.gateway.register.center.zookeeper;

import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.constants.BasicConst;
import com.sealand.common.utils.NetUtils;
import com.sealand.common.utils.TimeUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.junit.Test;
import java.util.HashMap;
import java.util.List;

import static com.sealand.common.constants.BasicConst.COLON_SEPARATOR;
import static com.sealand.common.constants.BasicConst.PATH_SEPARATOR;

/**
 * @author cgh
 * @create 2023-12-21
 * @desc
 */
public class ZookeeperTest {

    final static String REGISTER_CENTER_ZOOKEEPER_PREFIX = "/api-gateway/service";

    @Test
    public void test() throws Exception {
        ZookeeperRegisterCenter zookeeper = new ZookeeperRegisterCenter();
        zookeeper.init("127.0.0.1:2181", "dev");
        //注册节点
        //zookeeper.register(buildGatewayServiceDefinition(), buildGatewayServiceInstance());
        CuratorFramework client = zookeeper.getCuratorClient();
        String node = REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + buildGatewayServiceDefinition().getServiceId() + BasicConst.PATH_SEPARATOR +
                buildGatewayServiceInstance().getIp() + BasicConst.COLON_SEPARATOR + buildGatewayServiceInstance().getPort();
//        client.delete().forPath(node);
        //查询该下所有子节点
        List<String> pathList = client.getChildren().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + buildGatewayServiceDefinition().getServiceId() );
        //查询该节点下所有数据
        for (String path : pathList) {
            byte[] bytes = client.getData().forPath(REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + buildGatewayServiceDefinition().getServiceId() + PATH_SEPARATOR + path);
            System.out.println(new String(bytes));
        }
        while (true) {
            this.listenNode(client);
            Thread.sleep(2000);
        }
    }

    /*
     * //监听节点及子节点变化
     */
    void listenNode(CuratorFramework client) throws Exception {
        /*
         true表示用于配置是否把节点内容缓存起来，如果配置为true，客户端在接收到节点列表变更的同时，也能够获取到节点的数据内容
         （即：event.getData().getData()）ͺ如果为false 则无法取到数据内容（即：event.getData().getData()）
         */
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, REGISTER_CENTER_ZOOKEEPER_PREFIX + BasicConst.PATH_SEPARATOR + buildGatewayServiceDefinition().getServiceId(), true);
        /*
         * NORMAL:  普通启动方式, 在启动时缓存子节点数据
         * POST_INITIALIZED_EVENT：在启动时缓存子节点数据，提示初始化
         * BUILD_INITIAL_CACHE: 在启动时什么都不会输出
         * 在官方解释中说是因为这种模式会在start执行执行之前先执行rebuild的方法，而rebuild的方法不会发出任何事件通知。
         */
        pathChildrenCache.start(PathChildrenCache.StartMode.NORMAL);
        pathChildrenCache.getListenable().addListener((curatorFramework, event) -> {
            if (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
                System.out.println("子节点更新");
                System.out.println("节点:" + event.getData().getPath());
                System.out.println("数据" + new String(event.getData().getData()));
            } else if (event.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
                System.out.println("初始化操作");
            } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                System.out.println("删除子节点");
                System.out.println("节点:" + event.getData().getPath());
                System.out.println("数据" + new String(event.getData().getData()));
            } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                System.out.println("添加子节点");
                System.out.println("节点:" + event.getData().getPath());
                System.out.println("数据" + new String(event.getData().getData()));
            } else if (event.getType() == PathChildrenCacheEvent.Type.CONNECTION_SUSPENDED) {
                System.out.println("连接失效");
            } else if (event.getType() == PathChildrenCacheEvent.Type.CONNECTION_RECONNECTED) {
                System.out.println("重新连接");
            } else if (event.getType() == PathChildrenCacheEvent.Type.CONNECTION_LOST) {
                System.out.println("连接失效后稍等一会儿执行");
            }
        });
        while (true) {
        }
    }

    static ServiceInstance buildGatewayServiceInstance() {
        String localIp = NetUtils.getLocalIp();
        int port = 2182;
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId(localIp + COLON_SEPARATOR + port);
        serviceInstance.setIp("192.168.126.3");
        serviceInstance.setPort(8084);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }

    static ServiceDefinition buildGatewayServiceDefinition() {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setInvokerMap(new HashMap<>());
        serviceDefinition.setUniqueId("backend-http-server");
        serviceDefinition.setServiceId("backend-http-server");
        serviceDefinition.setEnvType("dev");
        return serviceDefinition;
    }
}
