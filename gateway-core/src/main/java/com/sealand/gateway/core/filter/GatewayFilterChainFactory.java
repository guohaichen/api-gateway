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
public class GatewayFilterChainFactory implements FilterFactory {

    /**
     * 过滤器map
     */
    private final Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    private static class SingletonInstance {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    public static GatewayFilterChainFactory getInstance() {
        return SingletonInstance.INSTANCE;
    }


    /**
     * 利用java spi机制扫描所有的filter
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
                //添加到过滤器集合
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
     * @param ctx 网关上下文，包含请求，响应以及rule
     * @return
     * @throws Exception
     * @desc 构建过滤器链
     */
    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        GatewayFilterChain filterChain = new GatewayFilterChain();
        List<Filter> filters = new ArrayList<>();
        Rule rule = ctx.getRule();
        if (rule != null) {
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while (iterator.hasNext()) {
                filterConfig = iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterConfigId = filterConfig.getId();
                if (StringUtils.isNotEmpty(filterConfigId) && getFilterInfo(filterConfigId) != null) {
                    Filter filter = getFilterInfo(filterConfigId);
                    filters.add(filter);
                }
            }
        }

        //todo 添加路由过滤器
        filters.add(new RouterFilter());
        //排序过滤器
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        //添加到链表中
        filterChain.addFilterList(filters);
        return filterChain;
    }

    @Override
    public Filter getFilterInfo(String filterId) throws Exception {
        return processorFilterIdMap.get(filterId);
    }
}
