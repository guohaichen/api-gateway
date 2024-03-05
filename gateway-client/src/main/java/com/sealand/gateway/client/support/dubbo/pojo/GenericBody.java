package com.sealand.gateway.client.support.dubbo.pojo;


import lombok.Data;

import java.io.Serializable;

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
    private String[] parametersName;
    private Object requestBody;

    public GenericBody(String serviceName, String methodName, String[] parameters, String[] parametersName) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameters = parameters;
        this.parametersName = parametersName;
    }

    public GenericBody(String serviceName, String methodName, String[] parameters, String[] parametersName, Object requestBody) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameters = parameters;
        this.parametersName = parametersName;
        this.requestBody = requestBody;
    }

    public GenericBody() {
    }
}
