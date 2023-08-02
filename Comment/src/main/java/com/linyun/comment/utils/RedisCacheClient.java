package com.linyun.comment.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.linyun.comment.pojo.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.linyun.comment.utils.RedisConstants.*;

/**
 * @author linyun
 * @since 2023/7/28 16:22
 */
@Slf4j
@Component
public class RedisCacheClient {

    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public RedisCacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 设置普通缓存
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    /*缓存雪崩：是指同一时间大量缓存同时失效或则redis服务宕机，导致大量请求到达数据库，带来压力
      ---->解决方案：1.给不同的key的TTL添加随机值 2.利用redis集群提升服务的可用性*/

    /**
     * 设置逻辑过期缓存
     *
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透：数据在缓存和数据库中都不存在 不断发起这样的请求，给数据库带来巨大的压力
     * --->解决方案：1.缓存""空值 2.布隆过滤 3.增强id的复杂度 避免被猜测id值
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithThroughPass(String keyPrefix, ID id, Class<R> type,
                                          Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //查询redis
        String json = stringRedisTemplate.opsForValue().get(key);
        //redis存在 直接返回
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            return null;
        }
        //redis不存在 查询数据库 添加redis后返回
        R r = dbFallback.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
        }
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 缓存击穿：也叫热点key问题，就是一个高并发访问并且缓存重建业务比较复杂的key突然失效了，无数的请求访问就会在瞬间给数据库带来巨大的压力
     * 解决方案：1.互斥锁
     *
     * @param id
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type,
                                    Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //查询redis
        String json = stringRedisTemplate.opsForValue().get(key);
        //redis存在 直接返回
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            return null;
        }
        R r = null;
        String lockKey = LOCK_ABSENT_KEY + id;
        try {
            boolean flag = tryLock(lockKey);
            if (!flag) {
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            //redis不存在 查询数据库 添加redis后返回
            r = dbFallback.apply(id);

            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);

                return null;
            }
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        return r;
    }

    /**
     * 缓存击穿：也叫热点key问题，就是一个高并发访问并且缓存重建业务比较复杂的key突然失效了，无数的请求访问就会在瞬间给数据库带来巨大的压力
     * 2.逻辑过期
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithLogicExpire(String keyPrefix, ID id, Class<R> type,
                                          Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String lockKey = LOCK_ABSENT_KEY + id;
        //查询redis
        String json = stringRedisTemplate.opsForValue().get(key);
        //redis为空或者为null 直接返回
        if (StrUtil.isBlank(json)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        //判断时间是否在现在之后
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        //缓存过期 重建
        boolean flag = tryLock(lockKey);
        if (flag) {
            if (expireTime.isAfter(LocalDateTime.now())) {
                return r;
            }
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                //调用线程池重建
                try {
                    R newR = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        return r;
    }


    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1");
        stringRedisTemplate.expire(key, 20, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
