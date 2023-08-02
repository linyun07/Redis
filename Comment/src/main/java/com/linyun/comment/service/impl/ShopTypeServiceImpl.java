package com.linyun.comment.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.linyun.comment.dto.Result;
import com.linyun.comment.pojo.Shop;
import com.linyun.comment.pojo.ShopType;
import com.linyun.comment.mapper.ShopTypeMapper;
import com.linyun.comment.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyun.comment.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.linyun.comment.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.linyun.comment.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private ShopTypeMapper shopTypeMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result getAllType() {
        String key = "cache:shop:type";
        //查询redis
        String redisShopType = stringRedisTemplate.opsForValue().get(key);
        //redis存在 直接返回
        if (StrUtil.isNotBlank(redisShopType)) {
            List<ShopType> shopType = JSONUtil.toList(redisShopType, ShopType.class);
            return Result.ok(shopType);
        }
        //redis不存在 查询数据库 添加redis后返回
        List<ShopType> allType = shopTypeMapper.getAllType();
        if (allType.isEmpty()) {
            return Result.fail("数据有误，请重试");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(allType), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(allType);
    }
}
