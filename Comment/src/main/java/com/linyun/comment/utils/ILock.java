package com.linyun.comment.utils;

/**
 * @author linyun
 * @since 2023/8/3 9:39
 */


public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec
     * @return
     */
    boolean tryLock(Long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();

}
