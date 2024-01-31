package com.sealand.gateway.core.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author cgh
 * @create 2024-01-12
 * @desc 创建 jedis连接池
 */
@Slf4j
public class JedisPoolUtil {
    private String host;
    private int port;
    private int maxActive;
    private int maxIdle;
    private int minIdle;

    private int maxWaitMillis;

    private JedisPool jedisPool;

    public static Lock lock = new ReentrantLock();

    public JedisPool getJedisPool() {
        if (jedisPool == null) {
            initialPool();
        }
        return jedisPool;
    }

    private void initialConfig() {
        try {
            Properties redisProperties = new Properties();
            //加载配置文件获取数据
            redisProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("gateway.properties"));
            host = redisProperties.getProperty("redis.host");
            port = Integer.parseInt(redisProperties.getProperty("redis.port"));
            maxActive = Integer.parseInt(redisProperties.getProperty("jedis.pool.max-active"));
            maxIdle = Integer.parseInt(redisProperties.getProperty("jedis.pool.max-idle"));
            minIdle = Integer.parseInt(redisProperties.getProperty("jedis.pool.min-idle"));
            maxWaitMillis = Integer.parseInt(redisProperties.getProperty("jedis.pool.max-wait"));
        } catch (Exception e) {
            log.debug("parse configure file error.");
        }
    }

    private void initialPool() {
        if (lock.tryLock()) {
            lock.lock();
            initialConfig();
            try {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(maxActive);
                config.setMaxIdle(maxIdle);
                config.setMinIdle(minIdle);
                config.setMaxWaitMillis(maxWaitMillis);
                jedisPool = new JedisPool(config, host, port);
                log.info("redis init ...");
            } catch (Exception e) {
                log.debug("init redis pool failed : {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        } else {
            log.debug("some other is init pool, just wait 1 second.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
