package com.sealand.gateway.client.support.dubbo.pojo;


import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author cgh
 * @create 2024-01-29
 * @desc dubbo泛化调用对象
 */
@Data
public class GenericBody implements Serializable {
    private static final long serialVersionUID = -2856472114032132964L;
    private String serviceName;
    private String methodName;
    private String[] parameters;

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameters() {
        return parameters;
    }

    public Object requestBody;

    public Object getRequestBody() {
        return requestBody;
    }

    public GenericBody(String serviceName, String methodName, String[] parameters) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameters = parameters;
    }

    public GenericBody() {
    }
}
