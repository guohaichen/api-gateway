package com.sealand.gateway.client.core.annotion;

import java.lang.annotation.*;

/**
 * @author cgh
 * @create 2023-11-13
 * @desc 加在服务方法上
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiInvoker {
    String path();
}
