package com.linyun.comment.basic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author linyun
 * @since 2023/7/27 11:53
 */

@SpringBootTest
public class RedisTemplateTest {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
   public  void stringTest() {
        stringRedisTemplate.opsForValue().set("name", "黄琳 ");
        String name = stringRedisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }

}
