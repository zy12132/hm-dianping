package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 多线程
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    // 为了执行 lua 脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 第一步执行 lua 脚本  将订单存放到阻塞队列
    @Override
    public Result seckillVoucher(Long voucherId) {
        // stringRedisTemplate.execute 执行 lua 脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(), voucherId.toString(),
                // UserHolder.getUser().getId().toString()
                "1"
        );

        // 判断返回的结果
        if (result.intValue() != 0) {
            return Result.fail(result.intValue() == 1 ? "库存不足" : "不能重复下单");
        }

        // 获取用户 ID，这里暂时使用 1L 代替
        Long userId = 1L;
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();

        if (!isLock) {
            // 获取锁失败
            return Result.fail("不能重复下单---------");
        }

        //拿到了锁
        try {
            // 设置订单唯一 id  UUID 也可以
            long orderId = redisIdWorker.nextId("order");

            // 封装到 voucherOrder 中  配置订单信息
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(userId);
            voucherOrder.setId(orderId);

            // 将订单信息存储在 mq 中
            rabbitTemplate.convertAndSend("hm-dianping", voucherOrder);
            return Result.ok(orderId);
        } finally {
                lock.unlock();
        }
    }

    // 从消息队列中取出消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "hm-dianping", durable = "true"),
            exchange = @Exchange(name = "hm-dianping", type = ExchangeTypes.FANOUT)
    ))
    public void createVouterOrder(VoucherOrder voucherOrder) {
        executor.submit(() -> {
            try {
                otherHandler(voucherOrder);
            } catch (Exception e) {
                log.error("处理订单时出现异常", e);
            }
        });
    }

    // 执行业务逻辑
    @Transactional(rollbackFor = Exception.class)
    public void otherHandler(VoucherOrder voucherOrder) {
        try {
            iSeckillVoucherService.update()
                    .setSql("stock = stock -1")
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)
                    .update();
            save(voucherOrder);
        } catch (Exception e) {
            log.error("订单处理失败: voucherOrder={}", voucherOrder, e);
            throw e; // 触发事务回滚
        }
    }
}