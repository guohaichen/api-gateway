package com.sealand.gateway.core.filter.dubbo;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.DubboServiceInvoker;
import com.sealand.common.config.DynamicConfigManager;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInvoker;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.filter.Filter;
import com.sealand.gateway.core.filter.FilterAspect;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.sealand.common.constants.FilterConst.*;

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
    public void doFilter(GatewayContext gatewayContext) {
        if ("dubbo".equals(gatewayContext.getProtocol())) {
            if (gatewayContext.getUniqueId() == null) {
                return;
            }
            ServiceDefinition serviceDefinition = DynamicConfigManager.getInstance().getServiceDefinition(gatewayContext.getUniqueId());
            Map<String, ServiceInvoker> invokerMap = serviceDefinition.getInvokerMap();

            //将请求路径uri提取出来，根据注册中心中的缓存找到对应接口，方法名，参数，转发；
            String path = gatewayContext.getRequest().getPath();
            ServiceInvoker serviceInvoker = invokerMap.get(path);
            GenericBody genericBody = new GenericBody(serviceInvoker.getInterfaceClass(), serviceInvoker.getMethodName(), serviceInvoker.getParameterTypes());
            String genericJson = JSON.toJSONString(genericBody);
            gatewayContext.getRequest().setBody(genericJson);
            log.info("gatewayContext:{}", JSON.toJSONString(gatewayContext));
            gatewayContext.getRequest().setModifyHost("localhost:10000");
            gatewayContext.getRequest().setModifyPath("/dubbo/generic");
        }
    }
}
