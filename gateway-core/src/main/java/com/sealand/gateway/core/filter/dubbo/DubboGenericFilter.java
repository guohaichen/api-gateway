package com.sealand.gateway.core.filter.dubbo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sealand.common.config.DynamicConfigManager;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInvoker;
import com.sealand.gateway.client.support.dubbo.pojo.GenericBody;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.filter.Filter;
import com.sealand.gateway.core.filter.FilterAspect;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.sealand.common.constants.FilterConst.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

/**
 * @author cgh
 * @create 2024-01-29
 * @desc
 */
@Slf4j
@FilterAspect(id = DUBBO_FILTER_ID, name = DUBBO_FILTER_NAME, order = DUBBO_FILTER_ORDER)
public class DubboGenericFilter implements Filter {

    // 请求 http://localhost:8088/http-server/post
    @Override
    public void doFilter(GatewayContext gatewayContext) throws JsonProcessingException {
        String DUBBO_PROTOCOL = "dubbo";
        String MODIFY_PATH = "/dubbo/generic";
        if (DUBBO_PROTOCOL.equals(gatewayContext.getProtocol())) {
            if (gatewayContext.getUniqueId() == null) {
                return;
            }
            ServiceDefinition serviceDefinition = DynamicConfigManager.getInstance().getServiceDefinition(gatewayContext.getUniqueId());
            String json = getJson(gatewayContext, serviceDefinition);
            gatewayContext.getRequest().setBody(json);
            gatewayContext.getRequest().getHeaders().set(CONTENT_LENGTH, json.length());
            //todo 因为泛化调用，在请求体中加了一些泛化调用的必要参数，使得content-length改变，这里应该在build时更新，暂时在这里修改。
            log.info("request body：{}", json);

            // 之前写死，现在改为从负载均衡加载，泛化调用接口写死
//            gatewayContext.getRequest().setModifyHost("localhost:10000");
            //访问路径设置为 dubbo泛化调用专用接口
            gatewayContext.getRequest().setModifyPath(MODIFY_PATH);
        }
    }

    //封装 GenericBody 方便后续泛化调用
    private static String getJson(GatewayContext gatewayContext, ServiceDefinition serviceDefinition) throws JsonProcessingException {
        Map<String, ServiceInvoker> invokerMap = serviceDefinition.getInvokerMap();

        //将请求路径uri提取出来，根据注册中心中的缓存找到对应接口，方法名，参数，转发；
        String path = gatewayContext.getRequest().getPath();
        ServiceInvoker serviceInvoker = invokerMap.get(path);

        GenericBody genericBody = new GenericBody(serviceInvoker.getInterfaceClass(), serviceInvoker.getMethodName(), serviceInvoker.getParameterTypes(), serviceInvoker.getParametersName());

        genericBody.setRequestBody(gatewayContext.getRequest().getBody());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(genericBody);
    }
}
