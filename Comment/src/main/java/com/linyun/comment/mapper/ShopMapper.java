package com.linyun.comment.mapper;

import com.linyun.comment.pojo.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopMapper extends BaseMapper<Shop> {
    /**
     * 根据id查商品
     * @param id
     * @return
     */
    Shop getShopById(Long id);
}
