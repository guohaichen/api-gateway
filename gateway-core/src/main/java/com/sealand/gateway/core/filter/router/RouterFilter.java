package com.sealand.gateway.core.filter.router;

import com.sealand.common.config.Rule;
import com.sealand.common.enums.ResponseCode;
import com.sealand.common.exception.ConnectException;
import com.sealand.common.exception.ResponseException;
import com.sealand.gateway.core.config.ConfigLoader;
import com.sealand.gateway.core.context.GatewayContext;
import com.sealand.gateway.core.filter.Filter;
import com.sealand.gateway.core.filter.FilterAspect;
import com.sealand.gateway.core.helper.AsyncHttpHelper;
import com.sealand.gateway.core.helper.ResponseHelper;
import com.sealand.gateway.core.response.GatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static com.sealand.common.constants.FilterConst.*;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc 路由过滤器链
 */
@Slf4j
@FilterAspect(id = ROUTER_FILTER_ID,
        name = ROUTER_FILTER_NAME,
        order = ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {
    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
        Request request = gatewayContext.getRequest().build();
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);

        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();

        if (whenComplete) {
            future.whenComplete((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            });
        } else {
            future.whenCompleteAsync((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            });
        }
    }

    private void complete(Request request,
                          Response response,
                          Throwable throwable,
                          GatewayContext gatewayContext) {
        gatewayContext.releaseRequest();

        Rule rule = gatewayContext.getRule();
        int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
        int confRetryTimes = rule.getRetryConfig().getTimes();

        if ((throwable instanceof TimeoutException
                || throwable instanceof IOException)
                && currentRetryTimes <= confRetryTimes) {
            doRetry(gatewayContext, currentRetryTimes);
            return;
        }

        try {
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                if (throwable instanceof TimeoutException) {
                    log.warn("complete time out {}", url);
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    gatewayContext.setThrowable(new ConnectException(throwable,
                            gatewayContext.getUniqueId(),
                            url, ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", t);
        } finally {
            gatewayContext.written();
            ResponseHelper.writeResponse(gatewayContext);
        }
    }


    private void doRetry(GatewayContext gatewayContext, int retryTimes) {
        System.out.println("当前重试次数为" + retryTimes);
        gatewayContext.setCurrentRetryTimes(retryTimes + 1);
        try {
            doFilter(gatewayContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
