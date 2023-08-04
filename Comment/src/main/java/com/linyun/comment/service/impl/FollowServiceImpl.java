package com.linyun.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.linyun.comment.dto.Result;
import com.linyun.comment.dto.UserDTO;
import com.linyun.comment.pojo.Follow;
import com.linyun.comment.mapper.FollowMapper;
import com.linyun.comment.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyun.comment.service.IUserService;
import com.linyun.comment.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private FollowMapper followMapper;
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result follow(Long id, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        if (isFollow) {
            //关注 新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add("follow:" + userId, id.toString());
            }
        } else {
            //取关 删除数据
            int i = followMapper.delByUserIdAndFollowUserId(userId, id);
            if (i > 0) {
                stringRedisTemplate.opsForSet().remove("follow:" + userId, id.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result getFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        int count = followMapper.listByUserId(userId, id);
        return Result.ok(count > 0);
    }

    @Override
    public Result commonFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        String userKey = "follow:" + userId;
        String targetKey = "follow:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(userKey, targetKey);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //解析出用户id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDTOList = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }
}
