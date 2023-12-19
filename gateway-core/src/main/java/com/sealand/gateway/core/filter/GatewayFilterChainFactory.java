package com.sealand.gateway.core.filter;

import com.sealand.common.config.Rule;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.filter.router.RouterFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc 过滤器链工厂类实现
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterFactory<Filter> {

    /**
     * 过滤器map，根据filter id 找到对应的map
     */
    private final Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    private static class SingletonInstance {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    public static GatewayFilterChainFactory getInstance() {
        return SingletonInstance.INSTANCE;
    }


    /**
     * 利用java spi机制扫描所有的filter，并将filter id作为key，filter 作为value添加到map；
     */
    public GatewayFilterChainFactory() {
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        Iterator<Filter> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            Filter filter = iterator.next();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            if (annotation != null) {
                String filterId = annotation.id();
                if (StringUtils.isEmpty(filterId)) {
                    filterId = filter.getClass().getName();
                }
                //添加到过滤map,key 为注解id，value为具体filter类
                log.info("load filter success, filter:{}, id:{}, name:{},order:{}", filter.getClass(), annotation.id(), annotation.name(), annotation.order());
                processorFilterIdMap.put(filterId, filter);
            }
        }
    }

    public static void main(String[] args) {
        log.info("----------测试扫描所有的filter---------");
        new GatewayFilterChainFactory();
    }

    /**
     * @param gatewayContext 网关上下文，包含请求，响应以及rule
     * @return
     * @throws Exception
     * @desc 构建过滤器链
     */
    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext gatewayContext) throws Exception {
        GatewayFilterChain filterChain = new GatewayFilterChain();
        List<Filter> filters = new ArrayList<>();
        Rule rule = gatewayContext.getRule();
        /* rule是配置中心的配置，
         *  "rules": [{
         *       "id": "001",
         *       "name": "规则001",
         *       "protocol": "http",
         *       "serviceId": "backend-http-server",
         *       "prefix": "/user",
         *       "paths": [
         *         "/http-server/ping",
         *         "/http-server/test"
         *       ],
         *       "filterConfigs": [{
         *           "id": "load_balance_filter",
         *           "config": {
         *             "load_balance": "RoundRobin"
         *           }
         *         }]
         *     }]
         */
        if (rule != null) {
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            //这里是根据filterConfigs的id，获取
            while (iterator.hasNext()) {
                filterConfig = iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterConfigId = filterConfig.getId();
                //根据配置的id，去 map中找到filter
                Filter filter = getFilterByFilterId(filterConfigId);
                if (StringUtils.isNotEmpty(filterConfigId) && filter != null) {
                    filters.add(filter);
                }
            }
        }

        // 最后增加路由filter
        filters.add(new RouterFilter());
        //排序过滤器
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        //添加到链表中
        filterChain.addFilterList(filters);
        return filterChain;
    }

    /**
     * 通过filterId获取filter，
     * @param filterId 配置中心传来的配置 中的rule规则中的 filterId;
     * @return
     * @throws Exception
     */
    @Override
    public Filter getFilterByFilterId(String filterId) throws Exception {
        return processorFilterIdMap.get(filterId);
    }
}
