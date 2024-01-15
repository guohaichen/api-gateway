package com.sealand.gateway.core.filter.flowCtl.core;

import com.sealand.gateway.core.redis.JedisPoolUtil;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author cgh
 * @create 2024-01-12
 * @desc redis分布式固定窗口限流
 */
public class RedisCountLimiter {

    private static final int FLOW_CTL_RESULT = 0;

    /**
     * lua 脚本实现原子操作
     */
    static String script = "local count = redis.call('incr',KEYS[1])\n" +
            "if count == 1 then\n" +
            "    redis.call('expire',KEYS[1],ARGV[1])\n" +
            "end\n" +
            "if count > tonumber(ARGV[2]) then\n" +
            "    return 0\n" +
            "end\n" +
            "return 1";

    public boolean tryAcquire(String key, int permits, int duration) {
        //连接池获取jedis连接
        Jedis jedis = new JedisPoolUtil().getJedisPool().getResource();
        //执行
        String redisScript = jedis.scriptLoad(script);
        Object result;
        try {
            //redis操作返回0，1；  1成功，0失败
            result = jedis.evalsha(redisScript, Collections.singletonList(key), Arrays.asList(String.valueOf(duration), String.valueOf(permits)));
            if (result == null) {
                return true;
            }
            return Integer.parseInt(result.toString()) != FLOW_CTL_RESULT;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                //关闭连接
                jedis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
