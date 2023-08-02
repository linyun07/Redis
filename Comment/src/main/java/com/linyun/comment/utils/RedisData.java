package com.linyun.comment.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author linyun
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
