package com.sealand.gateway.core.config;

import lombok.Data;

@Data
public class Config {
    private int port = 8888;

    private String applicationName = "api-gateway";

    private String registryHost = "http://127.0.0.1";

    private String registryPort = "2379";




    private String registryAddress ;

    public String getRegistryAddress() {
        return this.registryHost + ":" + this.registryPort;
    }

    private String env = "dev";

    //注册/配置中心实现动态配置;
    private String registerAndConfigCenter = "etcd";

    //netty
    private int eventLoopGroupBossNum = 1;

    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();

    private int maxContentLength = 64 * 1024 * 1024;

    //默认单异步模式
    private boolean singleAsync = true;

    //	Http Async 参数选项：

    //	连接超时时间
    private int httpConnectTimeout = 30 * 1000;

    //	请求超时时间
    private int httpRequestTimeout = 30 * 1000;

    //	客户端请求重试次数
    private int httpMaxRequestRetry = 2;

    //	客户端请求最大连接数
    private int httpMaxConnections = 10000;

    //	客户端每个地址支持的最大连接数
    private int httpConnectionsPerHost = 8000;

    //	客户端空闲连接超时时间, 默认60秒
    private int httpPooledConnectionIdleTimeout = 60 * 1000;

    //扩展.......
}
