package com.sealand.backend.http.server.controller;

import com.sealand.gateway.client.core.annotion.ApiInvoker;
import com.sealand.gateway.client.core.annotion.ApiService;
import com.sealand.gateway.client.core.config.ApiProperties;
import com.sealand.gateway.client.core.config.ApiProtocol;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        return message.name + ":" + message.age;
    }


    /**
     * ------------------请求方式与参数 测试 start----------------------
     **/
    //http请求： http://ip:port/http-server/param/name=""&age=“”
    @GetMapping("/http-server/param")
    @ApiInvoker(path = "/http-server/param")
    public String test1(Message msg) {
        return msg.name + ":" + msg.age;
    }

    //http请求： http://ip:port/http-server/pathVariable/{3}/{male}
    @GetMapping("/http-server/pathVariable/{id}/{gender}")
    @ApiInvoker(path = "/http-server/pathVariable")
    public String test2(@PathVariable("id") String id, @PathVariable("gender") String gender) {
        return id + "," + gender;
    }


    /**
     * ------------------请求方式与参数 测试 end----------------------
     **/

    @Data
    static class Message {
        private String name;
        private String age;
    }
}
