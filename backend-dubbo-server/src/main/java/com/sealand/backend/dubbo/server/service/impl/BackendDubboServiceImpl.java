package com.sealand.backend.dubbo.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sealand.backend.dubbo.server.service.BackendDubboService;
import com.sealand.gateway.client.core.annotion.ApiInvoker;
import com.sealand.gateway.client.core.annotion.ApiService;
import com.sealand.gateway.client.core.config.ApiProtocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author cgh
 * @create 2024-01-04
 * @desc dubbo service 服务提供者
 */
@Service
@Slf4j
@ApiService(serviceId = "backend-dubbo-server", protocol = ApiProtocol.DUBBO, patternPath = "/dubbo")
public class BackendDubboServiceImpl implements BackendDubboService {

    @ApiInvoker(path = "/dubbo-server/ping")
    @Override
    public String ping(String msg, String result) {
        return "pong:" + msg + result;
    }

    @ApiInvoker(path = "/dubbo-server/list")
    @Override
    public List<String> getUser(List<String> userList) {
        log.info("userList:\t{}", Arrays.toString(userList.toArray()));
        return Arrays.asList("jack", "max", "melody");
    }

    @Override
    @ApiInvoker(path = "/dubbo-server/map")
    public String mapTest(Map<String, String> kv) {
        return JSON.toJSONString(kv);
    }


}
