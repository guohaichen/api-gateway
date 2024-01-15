package com.sealand.gateway.client.support.dubbo;

import com.sealand.common.config.ServiceDefinition;
import com.sealand.common.config.ServiceInstance;
import com.sealand.common.utils.NetUtils;
import com.sealand.common.utils.TimeUtil;
import com.sealand.gateway.client.core.ApiAnnotationScanner;
import com.sealand.gateway.client.core.config.ApiProperties;
import com.sealand.gateway.client.support.AbstractClientRegisterManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.HashSet;
import java.util.Set;

import static com.sealand.common.constants.BasicConst.COLON_SEPARATOR;
import static com.sealand.common.constants.GatewayConst.DEFAULT_WEIGHT;

/**
 * @author cgh
 * @create 2023-11-14
 * @desc dubbo服务 注册到注册中心
 */
@Slf4j
public class DubboClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent> {

    private Set<Object> set = new HashSet<>();

    public DubboClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ServiceBeanExportedEvent) {
            ServiceBean serviceBean = ((ServiceBeanExportedEvent) applicationEvent).getServiceBean();
            registerDubbo(serviceBean);
        } else if (applicationEvent instanceof ApplicationStartedEvent) {
            log.info("dubbo api started");
        }
    }

    private void registerDubbo(ServiceBean serviceBean) {
        Object bean = serviceBean.getRef();
        if (set.contains(bean)) {
            return;
        }

        ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean, serviceBean);
        if (serviceDefinition == null) {
            return;
        }

        serviceDefinition.setEnvType(getApiProperties().getEnv());


        //服务实例
        ServiceInstance serviceInstance = new ServiceInstance();
        String localIp = NetUtils.getLocalIp();
        int port = serviceBean.getProtocol().getPort();
        String serviceInstanceId = localIp + COLON_SEPARATOR + port;
        String uniqueId = serviceDefinition.getUniqueId();
        String version = serviceDefinition.getVersion();

        serviceInstance.setServiceInstanceId(serviceInstanceId);
        serviceInstance.setUniqueId(uniqueId);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        serviceInstance.setVersion(version);
        serviceInstance.setWeight(DEFAULT_WEIGHT);
        register(serviceDefinition, serviceInstance);
    }
}
