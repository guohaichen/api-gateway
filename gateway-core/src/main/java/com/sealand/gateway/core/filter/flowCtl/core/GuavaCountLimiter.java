package com.sealand.gateway.core.filter.flowCtl.core;

import com.sealand.common.config.Rule;
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
    private RateLimiter rateLimiter;
    private double maxPermits;

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

        //todo 判空
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
