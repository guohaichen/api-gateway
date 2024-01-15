package com.sealand.gateway.core.context;

import com.sealand.common.config.Rule;
import com.sealand.common.utils.AssertUtil;
import com.sealand.gateway.core.request.GatewayRequest;
import com.sealand.gateway.core.response.GatewayResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

/**
 * 网关上下文，包含请求，响应以及规则
 */
public class GatewayContext extends BasicContext {

    private GatewayRequest request;

    private GatewayResponse response;

    private Rule rule;

    private int currentRetryTimes;

    /**
     * 构造函数
     *
     * @param protocol
     * @param nettyCtx
     * @param keepAlive
     */
    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive, GatewayRequest request, Rule rule, int currentRetryTimes) {
        super(protocol, nettyCtx, keepAlive);
        this.request = request;
        this.rule = rule;
        this.currentRetryTimes = currentRetryTimes;
    }

    /**
     * 建造者模式
     */
    public static class Builder {
        private String protocol;
        private ChannelHandlerContext nettyCtx;
        private boolean keepAlive;
        private GatewayRequest request;
        private Rule rule;

        private Builder() {

        }

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder setRequest(GatewayRequest request) {
            this.request = request;
            return this;
        }

        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public GatewayContext build() {
            AssertUtil.notNull(protocol, "protocol 不能为空");

            AssertUtil.notNull(nettyCtx, "nettyCtx 不能为空");

            AssertUtil.notNull(request, "request 不能为空");

            AssertUtil.notNull(rule, "rule 不能为空");
            return new GatewayContext(protocol, nettyCtx, keepAlive, request, rule, 0);
        }
    }

    @Override
    public GatewayRequest getRequest() {
        return request;
    }

    public void setRequest(GatewayRequest request) {
        this.request = request;
    }

    @Override
    public GatewayResponse getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = (GatewayResponse) response;
    }

    @Override
    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /**
     * 根据过滤器ID获取对应的过滤器配置信息
     *
     * @param filterId
     * @return
     */
    public Rule.FilterConfig getFilterConfig(String filterId) {
        return rule.getFilterConfig(filterId);
    }

    public String getUniqueId() {
        return request.getUniqueId();
    }

    /**
     * 重写父类释放资源方法，用于正在释放资源
     */
    public void releaseRequest() {
        if (requestReleased.compareAndSet(false, true)) {
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
    }

    public void setCurrentRetryTimes(int currentRetryTimes) {
        this.currentRetryTimes = currentRetryTimes;
    }

    /**
     * 获取原始的请求对象
     *
     * @return
     */
    public GatewayRequest getOriginRequest() {
        return request;
    }

    public int getCurrentRetryTimes() {
        return currentRetryTimes;
    }
}
