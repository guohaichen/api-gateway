package com.sealand.gateway.core.filter.flowCtl;

import com.alibaba.fastjson.JSONObject;
import com.sealand.common.config.Rule;
import com.sealand.gateway.core.filter.flowCtl.core.GuavaCountLimiter;
import com.sealand.gateway.core.filter.flowCtl.core.RedisCountLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.sealand.common.constants.FilterConst.*;

/**
 * @author cgh
 * @create 2024-01-11
 * @desc
 */
public class FlowCtlRuleByPath implements IGatewayFlowCtlRule {

    private String ServiceId;

    private String path;

    private RedisCountLimiter redisCountLimiter;

    public FlowCtlRuleByPath(String serviceId, String path, RedisCountLimiter redisCountLimiter) {
        ServiceId = serviceId;
        this.path = path;
        this.redisCountLimiter = redisCountLimiter;
    }

    private static final String LIMIT_MESSAGE = "请求过于频繁,请稍后重试";

    //缓存 k服务:路径 v流控 规则
    private static final ConcurrentHashMap<String, FlowCtlRuleByPath> servicePathMap = new ConcurrentHashMap<>();

    public static FlowCtlRuleByPath getInstance(String serviceId, String path) {
        //todo buffer
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(":").append(path).toString();
        FlowCtlRuleByPath flowCtlRuleByPath = servicePathMap.get(key);
        if (flowCtlRuleByPath == null) {
            flowCtlRuleByPath = new FlowCtlRuleByPath(serviceId, path, new RedisCountLimiter());
            servicePathMap.put(key, flowCtlRuleByPath);
        }
        return flowCtlRuleByPath;
    }

    @Override
    public void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId) {

        Map<String, Integer> configMap = JSONObject.parseObject(flowCtlConfig.getConfig(), Map.class);

        double duration = configMap.get(FLOW_CTL_LIMIT_DURATION);
        double permits = configMap.get(FLOW_CTL_LIMIT_PERMITS);

        boolean flag = true;
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(":").append(path).toString();
        //redis 分布式令牌桶限流
        if (FLOW_CTL_MODEL_DISTRIBUTED.equals(flowCtlConfig.getModel())) {
            flag = redisCountLimiter.tryAcquire(key, (int) permits, (int) duration);
        } else {
            //guava 单机令牌桶限流
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(serviceId, flowCtlConfig);
            if (guavaCountLimiter == null) {
                throw new RuntimeException("单机限流配置错误");
            } else {
                //令牌总数
                double count = Math.ceil(permits / (duration));
                flag = guavaCountLimiter.tryAcquire((int) count);
            }
        }
        if (!flag) {
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
