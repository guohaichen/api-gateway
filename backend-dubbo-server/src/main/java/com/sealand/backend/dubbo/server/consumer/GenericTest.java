package com.sealand.backend.dubbo.server.consumer;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author cgh
 * @create 2024-01-16
 * @desc dubbo泛化调用示例
 */
@RestController
public class GenericTest {
    @Reference(interfaceName = "com.sealand.backend.dubbo.server.service.BackendDubboService", generic = true)
    GenericService genericService;


    String methodName = "ping";

    @GetMapping("/generic")
    public String ping(@RequestParam String msg) {
        //传入需要调用的方法，参数类型列表，参数列表
        Object result = genericService.$invoke(methodName, new String[]{"java.lang.String"}, new Object[]{msg});
        System.out.println(result.toString());
        return ((String) result);
    }
}
