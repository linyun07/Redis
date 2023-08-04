package com.linyun.comment.service;

import com.linyun.comment.dto.Result;
import com.linyun.comment.pojo.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {
    /**
     * 关注或取关
     * @param id
     * @param isFollow
     * @return
     */
    Result follow(Long id, Boolean isFollow);
    /**
     * 查看用户是否关注
     * @param id
     * @return
     */
    Result getFollow(Long id);
    /**
     * 查看共同关注
     * @param id
     * @return
     */
    Result commonFollow(Long id);
}
