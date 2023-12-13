package com.sealand.gateway.core.filter;

import com.sealand.gateway.core.context.GatewayContext;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc
 */
public class RouterFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {

    }

    @Override
    public int getOrder() {
        return Filter.super.getOrder();
    }
}
