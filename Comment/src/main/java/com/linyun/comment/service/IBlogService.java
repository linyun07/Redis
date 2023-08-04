package com.linyun.comment.service;

import com.linyun.comment.dto.Result;
import com.linyun.comment.pojo.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 查看评论
     *
     * @param id
     * @return
     */
    Result queryBolgById(Long id);
    /**
     * 查询热门评论
     *
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);
    /**
     * 修改点赞数量
     *
     * @param id
     * @return
     */
    Result likeBlog(Long id);
    /**
     * 查看评价点赞用户
     * @param id
     * @return
     */
    Result queryLikesUser(Long id);


}
