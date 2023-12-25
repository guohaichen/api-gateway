package com.sealand.gateway.client.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author cgh
 * @create 2023-11-13
 * @desc 配置类  @ConfigurationProperties 将当前bean的属性与配置文件中以api开头的键值对绑定
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    private String registerAddress;

    private String env = "dev";

    private String registerType;
}
