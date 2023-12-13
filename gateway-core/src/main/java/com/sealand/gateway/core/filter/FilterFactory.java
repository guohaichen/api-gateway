package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc
 */
public interface FilterFactory {

    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;

    <T> T getFilterInfo(String filterId) throws Exception;
}
