package com.sealand.gateway.client.core.autoconfigure;

import com.sealand.gateway.client.core.config.ApiProperties;
import com.sealand.gateway.client.support.dubbo.DubboClientRegisterManager;
import com.sealand.gateway.client.support.springMVC.SpringMvcClientRegisterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author cgh
 * @create 2023-11-15
 * @desc 基于上下文自动装配dubbo/springMvc ClientRegisterManager
 * @Bean 方法：该类定义了两个@Bean方法，每个方法负责创建和配置特定的bean。
 * springMvcClientRegisterManager()：它仅在满足某些条件时创建SpringMvcClientRegisterManager类型的bean。
 * 这些条件包括类似Servlet、DispatcherServlet和WebMvcConfigurer等类的存在，以及不存在SpringMvcClientRegisterManager类型的bean。
 * 创建的bean使用ApiProperties实例进行配置。
 * <p>
 * dubboClientRegisterManager()：与第一个方法类似，它在满足某些条件时创建DubboClientRegisterManager类型的bean。
 * 这些条件包括ServiceBean类的存在以及不存在DubboClientRegisterManager类型的bean。
 * 创建的bean同样使用ApiProperties实例进行配置。
 */
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@ConditionalOnProperty(prefix = "api", name = "register-address")
public class ApiClientAutoConfiguration {

    @Autowired
    private ApiProperties apiProperties;

    @Bean
    @ConditionalOnProperty(name = "api.type", havingValue = "mvc")
    @ConditionalOnMissingBean(SpringMvcClientRegisterManager.class)
    public SpringMvcClientRegisterManager springMvcClientRegisterManager() {
        return new SpringMvcClientRegisterManager(apiProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "api.type", havingValue = "dubbo")
    @ConditionalOnMissingBean(DubboClientRegisterManager.class)
    public DubboClientRegisterManager dubboClientRegisterManager() {
        return new DubboClientRegisterManager(apiProperties);
    }

}
