package com.linyun.comment;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.linyun.comment.mapper.ShopMapper;
import com.linyun.comment.pojo.Shop;
import com.linyun.comment.utils.RedisData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.linyun.comment.utils.RedisConstants.*;

/**
 * @author linyun
 * @since 2023/7/28 11:52
 */

@SpringBootTest
public class CacheThroughPassTest {
    @Resource
    private ShopMapper shopMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    @Test
    public void selectById() {
        Long id = 1L;
//        queryWithThroughPass(id);
        //用互斥锁解决缓存击穿
//        queryWithMutex(id);
//        Shop shop = queryWithLogicExpire(id);
//        System.err.println(shop);
        saveShopForExpireTime(1L,20L);

    }

    /**
     * 缓存穿透
     *
     * @param id
     */
    private void queryWithThroughPass(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //查询redis
        String redisShop = stringRedisTemplate.opsForValue().get(key);
        //redis存在 直接返回
        if (StrUtil.isNotBlank(redisShop)) {
            Shop resultShop = JSONUtil.toBean(redisShop, Shop.class);
            System.out.println("从缓存中查找到数据" + resultShop);
        }
        if (redisShop != null) {
            System.out.println("没找到数据");
        }
        //redis不存在 查询数据库 添加redis后返回
        Shop dbShop = shopMapper.getShopById(id);
        if (dbShop == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            System.out.println("没找到数据");
        }
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(dbShop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        System.out.println("从数据库中查找到数据" + dbShop);
    }

    /**
     * 用互斥锁解决缓存击穿
     *
     * @param id
     */
    private Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String lockKey = LOCK_ABSENT_KEY + id;
        //查询redis
        String redisShop = stringRedisTemplate.opsForValue().get(key);
        //redis存在 直接返回
        if (StrUtil.isNotBlank(redisShop)) {
            Shop resultShop = JSONUtil.toBean(redisShop, Shop.class);
            System.out.println("从缓存中查找到数据" + resultShop);
            return resultShop;
        }
        if (redisShop != null) {
            System.out.println("没找到数据");
            return null;
        }
        Shop dbShop = null;
        try {
            if (!tryLock(lockKey)) {
                Thread.sleep(500);
                return queryWithMutex(id);
            }
            //redis不存在 查询数据库 添加redis后返回
            dbShop = shopMapper.getShopById(id);
            if (dbShop == null) {
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                System.out.println("没找到数据");
            }
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(dbShop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
        System.out.println("从数据库中查找到数据" + dbShop);
        return dbShop;
    }

    /**
     * 逻辑过期时间解决缓存击穿
     *
     * @param id
     */
    private Shop queryWithLogicExpire(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String lockKey = LOCK_ABSENT_KEY + id;
        //查询redis
        String redisShop = stringRedisTemplate.opsForValue().get(key);
        //redis为空或者为null 直接返回
        if (StrUtil.isBlank(redisShop)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(redisShop, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断时间是否在现在之后
        Shop resultShop=null;
        if (expireTime.isAfter(LocalDateTime.now())) {
             resultShop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
            System.out.println("从缓存中查找到数据" + resultShop);
            return resultShop;
        }
        //缓存过期 重建
        boolean flag = tryLock(lockKey);

            if (flag) {
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    //调用线程池重建
                    try {
                        this.saveShopForExpireTime(id, 20L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        unlock(lockKey);
                    }
                });
            }
        return resultShop;
    }


    private void saveShopForExpireTime(Long id, Long expireSecond) {
        Shop shop = shopMapper.getShopById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSecond));

        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
