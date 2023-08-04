package com.linyun.comment.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linyun.comment.dto.Result;
import com.linyun.comment.dto.UserDTO;
import com.linyun.comment.pojo.Blog;
import com.linyun.comment.pojo.User;
import com.linyun.comment.service.IBlogService;
import com.linyun.comment.service.IUserService;
import com.linyun.comment.utils.SystemConstants;
import com.linyun.comment.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 保存评论
     *
     * @param blog
     * @return
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    /**
     * 修改点赞数量
     *
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    /**
     * 查询用户评论
     *
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询热门评论
     *
     * @param current
     * @return
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * 查看评论
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryBolgById(@PathVariable Long id) {
        return blogService.queryBolgById(id);
    }

    /**
     * 查看评价点赞用户
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result queryLikesUser(@PathVariable Long id){
        return blogService.queryLikesUser(id);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }


}
