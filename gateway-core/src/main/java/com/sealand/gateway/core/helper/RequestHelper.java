package com.sealand.gateway.core.helper;

import com.sealand.common.config.*;
import com.sealand.common.constants.BasicConst;
import com.sealand.common.constants.GatewayConst;
import com.sealand.common.exception.ResponseException;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.request.GatewayRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.sealand.common.constants.BasicConst.COLON_SEPARATOR;
import static com.sealand.common.enums.ResponseCode.PATH_NO_MATCHED;

/**
 *
 */
@Slf4j
public class RequestHelper {

    public static GatewayContext buildContext(FullHttpRequest request, ChannelHandlerContext ctx) {

        //	构建请求对象GatewayRequest
        GatewayRequest gateWayRequest = buildRequest(request, ctx);

        //	根据请求对象里的uniqueId，获取资源服务信息(也就是服务定义信息)
        ServiceDefinition serviceDefinition = DynamicConfigManager.getInstance().getServiceDefinition(gateWayRequest.getUniqueId());
//        if ("dubbo".equals(serviceDefinition.getProtocol())){
//            String uri = gateWayRequest.getUri();
//            Map map = JSONUtil.parse(uri, Map.class);
//            if (map.get("serviceName")!=null&&map.get("methodName")!=null&&map.get("parameterTypes")!=null){
//                gateWayRequest.setModifyPath("/dubbo/generic?");
//            }
//        }

        //	根据请求对象获取服务定义对应的方法调用，然后获取对应的规则
        ServiceInvoker serviceInvoker = new HttpServiceInvoker();
        serviceInvoker.setInvokerPath(gateWayRequest.getPath());
        serviceInvoker.setTimeout(500);

        //根据请求对象获取规则
        Rule rule = getRule(gateWayRequest, serviceDefinition.getServiceId());

        //	构建自定义的GateWayContext对象
        GatewayContext gatewayContext = new GatewayContext(
                serviceDefinition.getProtocol(),
                ctx,
                HttpUtil.isKeepAlive(request),
                gateWayRequest,
                rule,
                0);


        //已经改为从配置中心动态获取 host和port；
//        gatewayContext.getRequest().setModifyHost("127.0.0.1:8080");

        return gatewayContext;
    }

    /**
     * 构建Request请求对象
     */
    private static GatewayRequest buildRequest(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {

        HttpHeaders headers = fullHttpRequest.headers();
        //	从header头获取必须要传入的关键属性 uniqueId
        String uniqueId = headers.get(GatewayConst.UNIQUE_ID);

        String host = headers.get(HttpHeaderNames.HOST);
        HttpMethod method = fullHttpRequest.method();
        String uri = fullHttpRequest.uri();
        String clientIp = getClientIp(ctx, fullHttpRequest);
        String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null : HttpUtil.getMimeType(fullHttpRequest).toString();
        Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);
        /*---------------------post请求 添加body------start-------*/
        String body = null;
        if (HttpMethod.POST.equals(fullHttpRequest.method())) {
            if (fullHttpRequest.content().isReadable()) {
                ByteBuf content = fullHttpRequest.content();
                body = content.toString(StandardCharsets.UTF_8);
            }
        }
        GatewayRequest gatewayRequest = new GatewayRequest(uniqueId,
                charset,
                clientIp,
                host,
                uri,
                method,
                contentType,
                headers,
                fullHttpRequest);
        /*---------------------post请求 添加body------- end------*/
        if (body != null) {
            gatewayRequest.setBody(body);
        }
        return gatewayRequest;
    }

    /**
     * 获取客户端ip
     */
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);

        String clientIp = null;
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(", "));
            if (values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }
        if (clientIp == null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }
        return clientIp;
    }

    /**
     * 获取rule对象
     *
     * @param gateWayRequest
     * @param serviceId
     * @return
     */
    private static Rule getRule(GatewayRequest gateWayRequest, String serviceId) {
        String key = serviceId + COLON_SEPARATOR + gateWayRequest.getPath();
        Rule rule = DynamicConfigManager.getInstance().getRuleByPath(key);

        if (rule != null) {
            return rule;
        }
        return DynamicConfigManager.getInstance().getRuleByServiceId(serviceId)
                .stream().filter(r -> gateWayRequest.getPath().startsWith(r.getPrefix()))
                .findAny().orElseThrow(() -> new ResponseException(PATH_NO_MATCHED));
    }

}
