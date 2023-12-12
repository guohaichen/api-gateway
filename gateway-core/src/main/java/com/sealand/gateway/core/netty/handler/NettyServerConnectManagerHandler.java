package com.sealand.gateway.core.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import com.sealand.common.utils.RemotingHelper;


/**
 * 连接管理器，管理连接对生命周期
 * ChannelDuplexHandler 可同时处理 出站和入站事件
 */
@Slf4j
public class NettyServerConnectManagerHandler extends ChannelDuplexHandler {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //当Channel注册到它的EventLoop并且能够处理I/O时调用
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddr);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //当Channel从它的EventLoop中注销并且无法处理任何I/O时调用
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPELINE: channelUnregistered {}", remoteAddr);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //当Channel处理于活动状态时被调用，可以接收与发送数据
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPELINE: channelActive {}", remoteAddr);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //不再是活动状态且不再连接它的远程节点时被调用
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPELINE: channelInactive {}", remoteAddr);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //当ChannelInboundHandler.fireUserEventTriggered()方法被调用时触发
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) { //有一段时间没有收到或发送任何数据
                final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                log.warn("NETTY SERVER PIPELINE: userEventTriggered: IDLE {}", remoteAddr);
                ctx.channel().close();
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //当ChannelHandler在处理过程中出现异常时调用
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.warn("NETTY SERVER PIPELINE: remoteAddr： {}, exceptionCaught {}", remoteAddr, cause);
        ctx.channel().close();
    }

}
