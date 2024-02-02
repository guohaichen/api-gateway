package com.sealand.backend.dubbo.server.generic.pojo;


import lombok.Data;

/**
 * @author cgh
 * @create 2024-01-29
 * @desc dubbo泛化调用对象
 */
@Data
public class GenericBody {
    private String serviceName;
    private String methodName;
    private Object[] parameters;

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

}