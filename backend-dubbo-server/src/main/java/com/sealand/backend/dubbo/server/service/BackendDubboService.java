package com.sealand.backend.dubbo.server.service;


import lombok.Data;

import java.util.List;

/**
 * @author cgh
 * @create 2024-01-04
 * @desc dubbo service
 */
public interface BackendDubboService {
    String ping(String msg, String result);

    List<String> getList(List<String> list);

    String getUser(User user);

    //泛型调用，实体类作为参数测试
    @Data
    class User {
        private String username;
        private Integer age;
    }
}


