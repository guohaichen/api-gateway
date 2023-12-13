package com.sealand.gateway.core.filter;

import java.lang.annotation.*;

/**
 * @author cgh
 * @create 2023-12-13
 * @desc
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FilterAspect {

    /**
     * 过滤器id
     * @return
     */
    String id();

    /**
     * 过滤器名称
     * @return
     */
    String name() default "";

    /**
     * 过滤器排序
     * @return
     */
    int order() default 0;

}
