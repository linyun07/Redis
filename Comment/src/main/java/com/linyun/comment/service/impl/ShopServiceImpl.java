package com.linyun.comment.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyun.comment.dto.Result;
import com.linyun.comment.mapper.ShopMapper;
import com.linyun.comment.pojo.Shop;
import com.linyun.comment.service.IShopService;
import com.linyun.comment.utils.RedisCacheClient;
import com.linyun.comment.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.linyun.comment.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.linyun.comment.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private ShopMapper shopMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisCacheClient cacheClient;

    @Override
    public Result selectById(Long id) {
        Shop shop = null;
        //缓存穿透
        if (id>=1&&id<=2) {
            //逻辑过期
            shop = cacheClient.queryWithLogicExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
//        cacheClient.queryWithThroughPass(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁
        shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("你查询的商品有误 请重试");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("店铺不存在 ，请重试一下");
        }
        //更新数据库
        updateById(shop);
        //删缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);
        return Result.ok(shopId);
    }

    @Override
    @Transactional
    public Result saveShop(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("店铺编号不存在 ，请重试一下");
        }
        //新增数据
        save(shop);
        //删缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);
        return Result.ok(shopId);
    }

    private void saveShopForExpireTime(Long id, Long expireSecond) {
        Shop shop = shopMapper.getShopById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSecond));

        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
}
