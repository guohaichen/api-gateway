package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc
 */
public interface FilterFactory<T> {

    GatewayFilterChain buildFilterChain(GatewayContext gatewayContext) throws Exception;

    T getFilterByFilterId(String filterId) throws Exception;
}
