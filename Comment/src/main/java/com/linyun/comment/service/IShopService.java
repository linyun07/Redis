package com.linyun.comment.service;

import com.linyun.comment.dto.Result;
import com.linyun.comment.pojo.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {
    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    Result selectById(Long id);
    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    Result updateShop(Shop shop);
    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    Result saveShop(Shop shop);
}
