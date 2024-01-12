package com.sealand.gateway.core.filter.flowCtl.core;

import com.sealand.common.config.Rule;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.shaded.com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author cgh
 * @create 2024-01-11
 * @desc guava 令牌桶单机限流
 * Guava RateLimiter提供了令牌桶算法实现：平滑突发限流(SmoothBurst)和平滑预热限流(SmoothWarmingUp)实现。
 */
public class GuavaCountLimiter {
    private final RateLimiter rateLimiter;

    //最大令牌数
    private final double maxPermits;

    /**
     * @param maxPermits 桶中最大的令牌数
     */
    public GuavaCountLimiter(double maxPermits) {
        this.maxPermits = maxPermits;
        this.rateLimiter = RateLimiter.create(maxPermits);
    }


    public GuavaCountLimiter(double maxPermits, long warmingUp) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits, warmingUp, TimeUnit.SECONDS);
    }

    public static ConcurrentHashMap<String, GuavaCountLimiter> resourceRateLimiterMap = new ConcurrentHashMap<>();


    public static GuavaCountLimiter getInstance(String serviceId, Rule.FlowCtlConfig flowCtlConfig) {
        if (flowCtlConfig == null
                || StringUtils.isEmpty(serviceId)
                || StringUtils.isEmpty(flowCtlConfig.getConfig())
                || StringUtils.isEmpty(flowCtlConfig.getValue())) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(":").append(flowCtlConfig.getValue()).toString();
        GuavaCountLimiter guavaCountLimiter = resourceRateLimiterMap.get(key);
        if (guavaCountLimiter == null) {
            guavaCountLimiter = new GuavaCountLimiter(50);
            resourceRateLimiterMap.putIfAbsent(key, guavaCountLimiter);
        }
        return guavaCountLimiter;
    }

    public boolean tryAcquire(int permits) {
        return rateLimiter.tryAcquire(permits);
    }

}
