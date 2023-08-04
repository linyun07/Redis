package com.linyun.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linyun.comment.dto.Result;
import com.linyun.comment.dto.UserDTO;
import com.linyun.comment.pojo.Blog;
import com.linyun.comment.mapper.BlogMapper;
import com.linyun.comment.pojo.User;
import com.linyun.comment.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyun.comment.service.IUserService;
import com.linyun.comment.utils.RedisConstants;
import com.linyun.comment.utils.SystemConstants;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private BlogMapper blogMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static final int LIKES_START_NUM = 0;
    public static final int LIKES_END_NUM = 4;

    /**
     * 查询是否评论
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBolgById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("评论出现问题，请重试");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }


    /**
     * 查询热门评论
     *
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 点赞
     *
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double isMember = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (isMember == null) {
            int success = blogMapper.updateAddLikedById(id);
            if (success > 0) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            int success = blogMapper.updateSubLikedById(id);
            if (success > 0) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result queryLikesUser(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        //查询redis点赞信息
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, LIKES_START_NUM, LIKES_END_NUM);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //解析出ids
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        //根据ids查询用户 封装成userDTO
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("order by field(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());


        return Result.ok(userDTOS);
    }



    /**
     * 设置IsLike用户前端显示
     *
     * @param blog
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return;
        }
        Long userId = user.getId();
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 设置评论的用户信息
     *
     * @param blog
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
