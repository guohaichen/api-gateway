package com.sealand.gateway.core.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.Rule;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.enums.ResponseCode;
import com.sealand.common.exception.NotFoundException;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.filter.Filter;
import com.sealand.gateway.core.filter.FilterAspect;
import com.sealand.gateway.core.request.GatewayRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.sealand.common.constants.FilterConst.*;

/**
 * @author cgh
 * @create 2023-12-14
 * @desc 负载均衡过滤器
 */

@Slf4j
@FilterAspect(id = LOAD_BALANCE_FILTER_ID,
        name = LOAD_BALANCE_FILTER_NAME,
        order = LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {
    @Override
    public void doFilter(GatewayContext gatewayContext) {
        String serviceId = gatewayContext.getUniqueId();
        //根据配置选择具体的负载均衡策略
        IGatewayLoadBalanceRule gatewayLoadBalanceRule = getLoadBalanceRule(gatewayContext);
        //选择注册中心中健康的服务（这里也是去DynamicConfigManager中找），具体是交给不同的负载均衡策略实现，例如轮询选择服务实例，随机选择实例等；
        ServiceInstance serviceInstance = gatewayLoadBalanceRule.chooseServiceById(serviceId);

        GatewayRequest request = gatewayContext.getRequest();
        if (serviceInstance != null && request != null) {
            log.info("服务地址:{},端口号:{}", serviceInstance.getAddress(), serviceInstance.getPort());
            String host = serviceInstance.getIp() + ":" + serviceInstance.getPort();
            //健康服务实例的地址
            request.setModifyHost(host);
        } else {
            log.warn("{} hasn't instance for providing", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
    }

    /**
     * 根据配置获取负载均衡器
     *
     * @param gatewayContext
     * @return
     */
    public IGatewayLoadBalanceRule getLoadBalanceRule(GatewayContext gatewayContext) {
        IGatewayLoadBalanceRule loadBalanceRule = null;
        Rule configRule = gatewayContext.getRule();
        if (configRule != null) {
            Set<Rule.FilterConfig> filterConfigs = configRule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while (iterator.hasNext()) {
                filterConfig = iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterConfigId = filterConfig.getId();
                if (filterConfigId.equals(LOAD_BALANCE_FILTER_ID)) {
                    String config = filterConfig.getConfig();
                    //轮询策略
                    String strategy = LOAD_BALANCE_STRATEGY_ROUND_ROBIN;
                    if (StringUtils.isNotEmpty(config)) {
                        Map<String, String> configMap = JSON.parseObject(config, Map.class);
                        strategy = configMap.getOrDefault(LOAD_BALANCE_KEY, strategy);
                    }
                    switch (strategy) {
                        case LOAD_BALANCE_STRATEGY_RANDOM:
                            loadBalanceRule = RandomLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                        case LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            loadBalanceRule = RoundRobinLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                        default:
                            log.warn("no load balance strategy for service: {}", configRule.getServiceId());
                            loadBalanceRule = RoundRobinLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                    }
                }
            }
        }
        return loadBalanceRule;
    }
}
