package com.sealand.gateway.client.core;

/**
 * @author cgh
 * @create 2023-11-13
 * @desc
 */
public enum ApiProtocol {
    HTTP("http", "http协议"),
    DUBBO("dubbo", "dubbo协议");

    private String code;

    //描述
    private String desc;

    ApiProtocol(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
