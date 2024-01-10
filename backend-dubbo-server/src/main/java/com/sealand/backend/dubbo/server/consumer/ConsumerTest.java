package com.sealand.backend.dubbo.server.consumer;

import com.sealand.backend.dubbo.server.service.BackendDubboService;
import com.sealand.backend.dubbo.server.service.impl.BackendDubboServiceImpl;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * @author cgh
 * @create 2024-01-10
 * @desc 服务消费组测试代码
 */
@RestController
public class ConsumerTest {
    @Reference
    private BackendDubboService dubboService;

    @GetMapping("/dubbo/ping")
    public String ping(@RequestParam String msg) {
        return dubboService.ping(msg);
    }

}
