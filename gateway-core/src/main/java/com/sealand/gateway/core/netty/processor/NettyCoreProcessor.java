package com.sealand.gateway.core.netty.processor;

import com.sealand.common.enums.ResponseCode;
import com.sealand.common.exception.BaseException;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.context.HttpRequestWrapper;
import com.sealand.gateway.core.filter.FilterFactory;
import com.sealand.gateway.core.filter.GatewayFilterChainFactory;
import com.sealand.gateway.core.helper.RequestHelper;
import com.sealand.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理基于netty服务器中的http请求
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    private final FilterFactory filterFactory = GatewayFilterChainFactory.getInstance();



    /**
     * 处理传入的http请求
     * @param wrapper 包含FullHttpRequest和 ChannelHandlerContext
     */
    @Override
    public void process(HttpRequestWrapper wrapper) {
        FullHttpRequest request = wrapper.getRequest();
        ChannelHandlerContext ctx = wrapper.getCtx();

        try {
            GatewayContext gatewayContext = RequestHelper.buildContext(request, ctx);
            //构建过滤器链并执行过滤器逻辑
            filterFactory.buildFilterChain(gatewayContext).executeFilter(gatewayContext);
        } catch (BaseException e) {
            log.error("process error {} {}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (Throwable t) {
            log.error("process unknown error", t);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            doWriteAndRelease(ctx, request, httpResponse);
        }

    }

    /**
     * 写回Http响应并释放资源
     * @param ctx          写入响应的通道
     * @param request      http请求
     * @param httpResponse 响应
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {
        ctx.writeAndFlush(httpResponse)
                .addListener(ChannelFutureListener.CLOSE); //释放资源后关闭channel
        ReferenceCountUtil.release(request);
    }
}
