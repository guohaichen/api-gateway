package com.sealand.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @author cgh
 * @create 2024-01-22
 * @desc dubbo
 */
@Setter
@Getter
public class DubboServiceInvoker extends AbstractServiceInvoker {

    //	注册中心地址
    private String registerAddress;

    //	接口全类名
    private String interfaceClass;

    //	方法名称
    private String methodName;

    //	参数类型的集合
    private String[] parameterTypes;

    //参数名称
    private String[] parametersName;

    //	dubbo服务的版本号
    private String version;

}
