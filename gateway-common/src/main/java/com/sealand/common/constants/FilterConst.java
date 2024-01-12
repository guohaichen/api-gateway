package com.sealand.common.constants;

/**
 * @author cgh
 * @create 2023-12-14
 * @desc 过滤器常量类
 */
public interface FilterConst {
    /**
     * 负载均衡过滤器常量
     */
    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";
    int LOAD_BALANCE_FILTER_ORDER = 100;

    String LOAD_BALANCE_KEY = "load_balance";
    String LOAD_BALANCE_STRATEGY_RANDOM = "Random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";
    /**
     * routeFilter常量
     */
    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_FILTER_ORDER = Integer.MAX_VALUE;

    String FLOW_CTL_FILTER_ID = "flow_ctl_filter";

    String FLOW_CTL_FILTER_NAME = "flow_ctl_filter";

    int FLOW_CTL_FILTER_ORDER = 50;

    String FLOW_CTL_TYPE_PATH = "path";

    String FLOW_CTL_TYPE_SERVICE = "service";

    /**
     * 以秒为单位限流
     */
    String FLOW_CTL_LIMIT_DURATION = "duration";
    /**
     * 允许请求的次数
     */
    String FLOW_CTL_LIMIT_PERMITS = "permits";


    /**
     * 分布式限流规则
     */
    String FLOW_CTL_MODEL_DISTRIBUTED = "distributed_flowCtl";
    /**
     * 单机限流规则
     */
    String FLOW_CTL_MODEL_SINGLETON = "singleton_flowCtl";

}
