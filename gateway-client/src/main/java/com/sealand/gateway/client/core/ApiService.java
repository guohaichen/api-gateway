package com.sealand.gateway.client.core;

import java.lang.annotation.*;

/**
 * @author cgh
 * @create 2023-11-13
 * @desc
 */

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiService {
    String serviceId();

    String version() default "1.0.0";

    ApiProtocol protocol();

    String patternPath();
}
