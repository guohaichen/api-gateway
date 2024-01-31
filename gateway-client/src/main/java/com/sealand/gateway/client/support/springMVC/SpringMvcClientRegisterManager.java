package com.sealand.gateway.client.support.springMVC;

import com.alibaba.fastjson.JSON;
import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.utils.NetUtils;
import com.sealand.common.utils.TimeUtil;
import com.sealand.gateway.client.core.ApiAnnotationScanner;
import com.sealand.gateway.client.core.config.ApiProperties;
import com.sealand.gateway.client.support.AbstractClientRegisterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sealand.common.constants.BasicConst.COLON_SEPARATOR;
import static com.sealand.common.constants.GatewayConst.DEFAULT_WEIGHT;

/**
 * @author cgh
 * @create 2023-11-14
 * @desc 将 springMVC 所有接口请求进行注册中心 注册
 */
@Slf4j
public class SpringMvcClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private Set<Object> set = new HashSet<>();

    @Autowired
    private ServerProperties serverProperties;

    public SpringMvcClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationStartedEvent) {
            try {
                RegisterSpringMvc();
            } catch (Exception e) {
                log.info("http register error : {}", e.getMessage());
                throw new RuntimeException(e);
            }
            log.info("http api started");
        }
    }

    private void RegisterSpringMvc() {
        //拿到所有的 mvc handler
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RequestMappingHandlerMapping.class, true, false);

        for (RequestMappingHandlerMapping requestMapping : allRequestMappings.values()) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMapping.getHandlerMethods();

            for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = handlerMethodEntry.getValue();
                Class<?> clazz = handlerMethod.getBeanType();

                Object bean = applicationContext.getBean(clazz);

                if (set.contains(bean)) {
                    continue;
                }
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);

                if (serviceDefinition == null) {
                    continue;
                }
                serviceDefinition.setEnvType(getApiProperties().getEnv());

                //服务实例
                ServiceInstance serviceInstance = new ServiceInstance();
                String localIp = NetUtils.getLocalIp();
                Integer port = serverProperties.getPort();
                String serviceInstancedId = localIp + COLON_SEPARATOR + port;
                String uniqueId = serviceDefinition.getUniqueId();
                String version = serviceDefinition.getVersion();

                serviceInstance.setServiceInstanceId(serviceInstancedId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setVersion(version);
                serviceInstance.setWeight(DEFAULT_WEIGHT);
                log.info("service definition register:{}", JSON.toJSONString(serviceDefinition));
                //注册
                register(serviceDefinition, serviceInstance);
            }

        }

    }

}


