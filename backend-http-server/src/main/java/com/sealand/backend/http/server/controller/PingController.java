package com.sealand.backend.http.server.controller;

import com.sealand.gateway.client.core.ApiInvoker;
import com.sealand.gateway.client.core.ApiProperties;
import com.sealand.gateway.client.core.ApiProtocol;
import com.sealand.gateway.client.core.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
}
