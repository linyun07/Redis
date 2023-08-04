package com.linyun.comment.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author linyun
 * @since 2023/8/2 10:49
 */

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    public static final long BEGIN_TIMEsTAMP = 1690973756;
    /**
     * 位数
     */
    public static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Long nextId(String keyPrefix) {
        //生成时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timeTamp = System.currentTimeMillis() - BEGIN_TIMEsTAMP;
        //生成序列号
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        stringRedisTemplate.expire("icr:" + keyPrefix + ":" + date, 10L, TimeUnit.SECONDS);
        //拼接返回

        return timeTamp << COUNT_BITS | count;
    }

}
