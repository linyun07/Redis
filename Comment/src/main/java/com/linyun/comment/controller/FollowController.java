package com.linyun.comment.controller;


import com.linyun.comment.dto.Result;
import com.linyun.comment.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    /**
     * 关注或取关
     * @param id
     * @param isFollow
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long id, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(id, isFollow);
    }

    /**
     * 查看用户是否关注
     * @param id
     * @return
     */
    @GetMapping("/or/not/{id}")
    public Result getFollow(@PathVariable("id") Long id) {
        return followService.getFollow(id);
    }

    /**
     * 查看共同关注
     * @param id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable Long id){
        return followService.commonFollow(id);
    }
}
