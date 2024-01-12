package com.sealand.gateway.core.filter.flowCtl.core;

import com.sealand.gateway.core.redis.JedisPoolUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author cgh
 * @create 2024-01-12
 * @desc redis分布式固定窗口限流
 */
public class RedisCountLimiter {

    /**
     * lua 脚本实现原子操作
     */
    static String redisScript = "" +
            "local count = redis.call(\"incr\",KEYS[1])\n" +
            "if count == 1 then\n" +
            "    redis.call('expire',KEYS[1],ARGV[2])\n" +
            "end\n" +
            "if count > tonumber(ARGV[1]) then\n" +
            "    return 0\n" +
            "end\n" +
            "return 1";

    public Boolean tryAcquire(String key, int permits, int duration) {
        //连接池获取jedis连接
        Jedis jedis = new JedisPoolUtil().getJedisPool().getResource();
        //执行
        jedis.scriptLoad(redisScript);
        Object evalsha = null;
        try {
            evalsha = jedis.evalsha(redisScript, Collections.singletonList(key), Arrays.asList(String.valueOf(permits), String.valueOf(duration)));
        } catch (Exception e) {
            //关闭连接
            jedis.close();
            throw new RuntimeException(e);
        }
        return evalsha == null;
    }
}
