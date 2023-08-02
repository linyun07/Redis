package com.linyun.comment.basic;

import com.linyun.comment.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author linyun
 * @since 2023/7/27 11:53
 */

@SpringBootTest
public class RedisTemplateTest {
    @Resource
    private RedisIdWorker redisIdWorker;
    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 测试唯一Id
     */
    @Test
    void testIdWork() throws Exception {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 10; i++) {

                System.out.println(redisIdWorker.nextId("test"));
            }
            latch.countDown();
        };
        LocalDateTime start = LocalDateTime.now();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        System.out.println("开始时间：" + start + "结束时间：" + LocalDateTime.now());
    }

    @Test
    public void stringTest() {
        stringRedisTemplate.opsForValue().set("name", "黄琳 ");
        String name = stringRedisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }

    //  mysql索引需要维护B+树的结构 频繁插入不连续的id 维护B+树的成本会很大
    @Test
    void showTimeSecond() {
        System.out.println(LocalDate.now());
        System.out.println(System.currentTimeMillis() - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    }
}
