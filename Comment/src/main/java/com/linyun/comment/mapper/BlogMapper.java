package com.linyun.comment.mapper;

import com.linyun.comment.pojo.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface BlogMapper extends BaseMapper<Blog> {

    int updateAddLikedById(Long id);
    int updateSubLikedById(Long id);
}
