package com.linyun.comment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linyun.comment.dto.LoginFormDTO;
import com.linyun.comment.dto.Result;
import com.linyun.comment.pojo.User;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {
    /**
     * 发送手机验证码
     */
    Result sendCode(String phone);

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    Result login(LoginFormDTO loginForm);
}
