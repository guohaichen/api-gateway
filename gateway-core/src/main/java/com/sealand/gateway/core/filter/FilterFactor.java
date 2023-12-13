package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc
 */
public interface FilterFactor {

    GatewayFilterChain buildFilterChain(GatewayContext cxt) throws Exception;

    <T> T getFilterInfo(String filterId) throws Exception;
}
