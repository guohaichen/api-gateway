package com.sealand.backend.dubbo.server.generic.controller;

import com.alibaba.fastjson.JSONObject;
import com.sealand.gateway.client.support.dubbo.pojo.GenericBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cgh
 * @create 2024-01-24
 * @desc http请求dubbo的具体实现，mvc作为代理层，提供统一访问接口，底层使用dubbo泛型调用；
 */
@RequestMapping("/dubbo")
@RestController
@Slf4j
public class GenericController {

    @Value("${dubbo.registry.address}")
    private String registerAddress;

    @Value("${dubbo.application.name}")
    private String dubboApplicationName;

    //todo 泛化调用目前参数支持较少，
    @PostMapping("/generic")
    public Object handleHttpRequest(@RequestBody GenericBody genericBody) {
        log.info("genericBody : {} ", genericBody);

        GenericService genericService = getGenericService(genericBody);
        //解析参数
        String[] parametersName = genericBody.getParametersName();
        Object[] parameterObject = new Object[parametersName.length];
        JSONObject jsonObject = JSONObject.parseObject(genericBody.getRequestBody().toString());

        for (int i = 0; i < parametersName.length; i++) {
            parameterObject[i] = jsonObject.get(parametersName[i]);
        }
        // api 泛化调用
        return genericService.$invoke(genericBody.getMethodName(), genericBody.getParameters(), parameterObject);
    }

    //dubbo 提供的 调用泛型接口的 api
    private GenericService getGenericService(GenericBody genericBody) {
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
        referenceConfig.setInterface(genericBody.getServiceName());
        referenceConfig.setGeneric("true");

        /* ReferenceConfig实例很重，封装了与注册中心的连接以及与提供者的连接，
        需要缓存，否则重复生成ReferenceConfig可能造成性能问题并且会有内存和连接泄漏。 */
        ReferenceConfigCache referenceConfigCache = ReferenceConfigCache.getCache();
        return referenceConfigCache.get(referenceConfig);
    }

}
