package com.sealand.backend.dubbo.server.controller;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cgh
 * @create 2024-01-24
 * @desc 网关请求dubbo的具体实现，mvc作为代理层，统一访问接口，底层使用dubbo泛型调用；
 */
@RequestMapping("/dubbo")
@RestController
public class GenericController {

    @Value("${dubbo.registry.address}")
    private String registerAddress;

    @Value("${dubbo.application.name}")
    private String dubboApplicationName;



    @GetMapping("/generic")
    public Object handleHttpRequest(@RequestParam String serviceName, @RequestParam String methodName, @RequestParam Object[] parameters) {
        //api泛型调用
        ApplicationConfig application = new ApplicationConfig();
        application.setName(dubboApplicationName);
        //创建服务引用配置
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(registerAddress);

        referenceConfig.setApplication(application);
        referenceConfig.setRegistry(registry);
        //泛化调用
        referenceConfig.setInterface(serviceName);
        referenceConfig.setGeneric("true");

        //获取服务，由于是泛化调用，所以获取的一定是GenericService类型
        GenericService genericService = referenceConfig.get();

        // 基本类型以及Date,List,Map等不需要转换，直接调用
        return genericService.$invoke(methodName, new String[]{"java.lang.String"},
                parameters);
    }

}
