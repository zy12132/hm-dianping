-- 订单id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 优惠券key  构建存储当前优惠券库存数量的键名。
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key  构建存储已成功下单用户ID集合的键名。
local orderKey = 'seckill:order:' .. voucherId
-- 判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end
-- 判断用户是否下单
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end
-- 扣减库存
redis.call('incrby', stockKey, -1)
-- 将userId存入当前优惠券的set集合
redis.call('sadd', orderKey, userId)
return 0

--local stockKey = KEYS[1]       -- 库存键（如：stock:activity123）
--local orderKey = KEYS[2]      -- 用户订单记录键（如：order:user:activity123）
--local userId = ARGV[1]        -- 用户ID
--
---- 1. 检查用户是否已下单（原子性操作）
--if redis.call('SISMEMBER', orderKey, userId) == 1 then
--    return 2 -- 已存在订单
--end
--
---- 2. 原子性扣减库存
--local stock = redis.call('INCRBY', stockKey, -1)
--if stock < 0 then
--    redis.call('INCRBY', stockKey, 1) -- 库存不足，回滚
--    return 1
--end
--
---- 3. 记录用户已下单（防止并发重复）
--redis.call('SADD', orderKey, userId)
---- 可选：设置订单记录的过期时间（如活动结束后自动清理）
--redis.call('EXPIRE', orderKey, 86400)
--
--return 0 -- 成功