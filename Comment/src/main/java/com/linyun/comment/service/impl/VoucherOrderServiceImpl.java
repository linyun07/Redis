package com.linyun.comment.service.impl;

import com.linyun.comment.dto.Result;
import com.linyun.comment.mapper.SeckillVoucherMapper;
import com.linyun.comment.mapper.VoucherMapper;
import com.linyun.comment.pojo.SeckillVoucher;
import com.linyun.comment.pojo.VoucherOrder;
import com.linyun.comment.mapper.VoucherOrderMapper;
import com.linyun.comment.service.ISeckillVoucherService;
import com.linyun.comment.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyun.comment.utils.RedisConstants;
import com.linyun.comment.utils.RedisIdWorker;
import com.linyun.comment.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private ISeckillVoucherService seckillVoucherService;


    @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(voucher.getBeginTime())) {
            return Result.fail("抢购还没开始");
        }
        if (!now.isBefore(voucher.getEndTime())) {
            return Result.fail("抢购还已经结束了，欢迎下次再来");
        }
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足，请下次尽快哦");
        }
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            //获取事务有关代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    @Override
    public Result createVoucherOrder(Long voucherId) {
        //查询用户是否购买过优惠券
        Long userId = UserHolder.getUser().getId();
        int orderCount = voucherOrderMapper.selectAllByUserIdAndVoucherId(userId, voucherId);
        if (orderCount > 0) {
            return Result.fail("请不要重复购买哦！！");
        }
        // 更新优惠券库存
        seckillVoucherMapper.updateStockInt(voucherId);
        //新增订单
        VoucherOrder order = new VoucherOrder();
        Long orderId = redisIdWorker.nextId("cache:order");
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        save(order);

        return Result.ok(orderId);
    }
}
