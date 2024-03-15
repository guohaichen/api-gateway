package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc 过滤器链类
 */
@Slf4j
public class GatewayFilterChain {

    private final List<Filter> filters = new ArrayList<>();

    //新增过滤器
    public GatewayFilterChain addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public void addFilterList(List<Filter> filterList) {
        filters.addAll(filterList);
    }

    @Trace
    public void executeFilter(GatewayContext gatewayContext) {
        if (filters.isEmpty()) {
            return;
        }
        try {
            for (Filter filter : filters) {
                filter.doFilter(gatewayContext);
                if (gatewayContext.isTerminated()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.info("doFilter failed, error : {}", e.getMessage());
        }
    }


}
