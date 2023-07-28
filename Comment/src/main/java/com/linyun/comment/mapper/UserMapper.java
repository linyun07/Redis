package com.linyun.comment.mapper;

import com.linyun.comment.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 登陆时 根据手机号码找用户
     * @param phone
     * @return
     */
    User getUserByPhone(String phone);
}
