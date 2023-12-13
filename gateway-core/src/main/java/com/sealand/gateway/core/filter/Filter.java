package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc
 */
public interface Filter {

    void doFilter(GatewayContext ctx) throws Exception;

    default int getOrder() {
        FilterAspect filterAspect = this.getClass().getAnnotation(FilterAspect.class);
        if (filterAspect != null) {
            return filterAspect.order();
        }
        return Integer.MAX_VALUE;
    }
}
