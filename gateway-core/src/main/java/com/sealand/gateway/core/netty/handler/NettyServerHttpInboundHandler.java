package com.sealand.gateway.core.netty.handler;

import com.sealand.gateway.core.context.HttpRequestWrapper;
import com.sealand.gateway.core.netty.processor.NettyProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 自定义netty 入站处理逻辑，将消息交给nettyProcessor处理；
 */
public class NettyServerHttpInboundHandler extends ChannelInboundHandlerAdapter {
    private final NettyProcessor nettyProcessor;

    public NettyServerHttpInboundHandler(NettyProcessor nettyProcessor) {
        this.nettyProcessor = nettyProcessor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
        httpRequestWrapper.setCtx(ctx);
        httpRequestWrapper.setRequest(request);

        nettyProcessor.process(httpRequestWrapper);
    }
}
