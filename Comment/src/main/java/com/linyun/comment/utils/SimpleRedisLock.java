package com.linyun.comment.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author linyun
 * @since 2023/8/3 9:41
 */


public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public static final String KEY_PREFIX = "lock:";
    public static final String ID_PREFIX = UUID.randomUUID().toString(true) + ":";

    @Override
    public boolean tryLock(Long timeoutSec) {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    @Override
    public void unLock() {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        String redisId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        //判断锁内容是否一致
        if (threadId.equalsIgnoreCase(redisId)) {
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
