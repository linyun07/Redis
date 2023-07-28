package com.linyun.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyun.comment.dto.LoginFormDTO;
import com.linyun.comment.dto.Result;
import com.linyun.comment.dto.UserDTO;
import com.linyun.comment.mapper.UserMapper;
import com.linyun.comment.pojo.User;
import com.linyun.comment.service.IUserService;
import com.linyun.comment.utils.RegexUtils;
import com.linyun.comment.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.linyun.comment.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone) {
        //判断手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机格式有误");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到 Redis中
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("短信验证码是：" + code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("电话号码错误，请校验");
        }
        String redisCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (redisCode == null || !(redisCode.equals(loginForm.getCode()))) {
            return Result.fail("验证码错误");
        }
        User user = userMapper.getUserByPhone(phone);
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        //随机生成token 作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //将user对象转化为Hash对象存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //把用户存储到redis
        String key = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(key, userMap);
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //删除验证码
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setNickName("linyun" + RandomUtil.randomString(8));
        //保存用户到数据库
        save(user);
        return user;
    }
}
