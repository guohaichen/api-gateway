package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc 过滤器链类
 */
@Slf4j
public class GatewayFilterChain {

    private final List<Filter> filters = new LinkedList<>();

    //新增过滤器
    public GatewayFilterChain addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public void addFilterList(List<Filter> filterList) {
        filters.addAll(filterList);
    }

    public GatewayContext doFilter(GatewayContext ctx) {
        if (filters.isEmpty()) {
            return ctx;
        }
        try {
            for (Filter filter : filters) {
                filter.doFilter(ctx);
            }
        } catch (Exception e) {
            log.info("doFilter fail, error : {}", e.getMessage());
        }
        return ctx;
    }


}
