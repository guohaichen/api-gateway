package com.sealand.backend.http.server.controller;

import com.sealand.gateway.client.core.annotion.ApiInvoker;
import com.sealand.gateway.client.core.annotion.ApiService;
import com.sealand.gateway.client.core.config.ApiProperties;
import com.sealand.gateway.client.core.config.ApiProtocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {

    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() {
        log.info("apiProperties: {}", apiProperties);
        return "pong";
    }

    @ApiInvoker(path = "/http-server/post")
    @PostMapping("/http-server/post")
    public String postTest(@RequestBody Message message) {
        String result = message.name + ":" + message.age;
        return result;
    }


    static class Message {
        private String name;
        private String age;
    }
}
