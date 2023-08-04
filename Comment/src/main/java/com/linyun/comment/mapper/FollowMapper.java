package com.linyun.comment.mapper;

import com.linyun.comment.dto.UserDTO;
import com.linyun.comment.pojo.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 取关
     * @param userId
     * @param followUserId
     * @return
     */
    int delByUserIdAndFollowUserId(@Param("userId")Long userId,@Param("followUserId") Long followUserId);

    /**
     * 查询共同关注 以及是否关注该用户
     * @param userId
     * @return
     */
    int listByUserId(@Param("userId")Long userId,@Param("followUserId") Long followUserId);



}
