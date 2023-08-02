package com.linyun.comment.mapper;

import com.linyun.comment.pojo.ShopType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopTypeMapper extends BaseMapper<ShopType> {
    /**
     * 查询标题
     * @return
     */
    List<ShopType> getAllType();
}
