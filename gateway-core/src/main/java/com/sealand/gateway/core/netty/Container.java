package com.sealand.gateway.core.netty;

import com.sealand.gateway.core.config.Config;
import com.sealand.gateway.core.netty.processor.NettyCoreProcessor;
import com.sealand.gateway.core.netty.processor.NettyProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * netty server 和 client 容器；
 * 初始化netty server 和 client
 */
@Slf4j
public class Container implements LifeCycle {
    private final Config config;

    private NettyHttpServer nettyHttpServer;

    private NettyHttpClient nettyHttpClient;

    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void init() {
        this.nettyProcessor = new NettyCoreProcessor();

        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);

        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("netty-gateway started!");
    }

    @Override
    public void shutdown() {
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
