package com.sealand.backend.dubbo.server.service.impl;

import com.sealand.backend.dubbo.server.service.BackendDubboService;
import org.apache.dubbo.config.annotation.Service;

/**
 * @author cgh
 * @create 2024-01-04
 * @desc dubbo service 服务提供者
 */
@Service
//@ApiService(serviceId = "backend-dubbo-server", protocol = ApiProtocol.DUBBO, patternPath = "/dubbo-server/**")
public class BackendDubboServiceImpl implements BackendDubboService {


//    @ApiInvoker(path = "/dubbo-server/ping")
    @Override
    public String ping(String msg) {
        return "pong: " + msg;
    }
}
