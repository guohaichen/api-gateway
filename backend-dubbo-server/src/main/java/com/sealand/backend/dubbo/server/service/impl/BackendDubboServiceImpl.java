package com.sealand.backend.dubbo.server.service.impl;

import com.sealand.backend.dubbo.server.service.BackendDubboService;
import com.sealand.gateway.client.core.annotion.ApiInvoker;
import com.sealand.gateway.client.core.annotion.ApiService;
import com.sealand.gateway.client.core.config.ApiProtocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;

import java.util.Arrays;
import java.util.List;

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
    public List<String> getList(List<String> list) {
        log.info("userList:\t{}", Arrays.toString(list.toArray()));
        list.add("王多鱼");
        return list;
    }
    /**
     实体类在 dubbo 过滤器 层面应该解析为map，目前没有做，后面再看。<b>目前泛化调用只支持基本类型；</b>
     */
    @Override
    @ApiInvoker(path = "/dubbo-server/user")
    public String getUser(User user) {
        return user.getUsername() + ":" + user.getAge();
    }
}
