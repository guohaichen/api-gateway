package com.sealand.gateway.core.netty;

import com.sealand.gateway.core.config.Config;
import com.sealand.gateway.core.netty.handler.NettyServerConnectManagerHandler;
import com.sealand.gateway.core.netty.processor.NettyProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.sealand.common.utils.RemotingUtil;
import com.sealand.gateway.core.netty.handler.NettyServerHttpInboundHandler;

import java.net.InetSocketAddress;

@Slf4j
public class NettyHttpServer implements LifeCycle {
    private final Config config;
    private final NettyProcessor nettyProcessor;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    @Getter
    private EventLoopGroup eventLoopGroupWorker;


    public NettyHttpServer(Config config, NettyProcessor nettyProcessor) {
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        init();
    }


    @Override
    public void init() {
        this.serverBootstrap = new ServerBootstrap();
        //判断os 是否支持epoll
        if (useEpoll()) {
            log.info("netty serverBootstrap start, using epoll;");
            //EpollEventLoopGroup
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-group"));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-group"));
        } else {
            log.info("netty serverBootstrap start, not supports epoll;");
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-group"));
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-group"));
        }
    }

    public boolean useEpoll() {
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        this.serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getPort()))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(
                                new HttpServerCodec(), //http编解码
                                new HttpObjectAggregator(config.getMaxContentLength()), //请求报文聚合成FullHttpRequest
                                new NettyServerConnectManagerHandler(),
                                new NettyServerHttpInboundHandler(nettyProcessor)
                        );
                    }
                });

        try {
            this.serverBootstrap.bind().sync();
            log.info("server startup on port {}", this.config.getPort());
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void shutdown() {
        if (eventLoopGroupBoss != null) {
            eventLoopGroupBoss.shutdownGracefully();
        }

        if (eventLoopGroupWorker != null) {
            eventLoopGroupWorker.shutdownGracefully();
        }
    }
}
