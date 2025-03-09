package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key , Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key , Object value, Long time, TimeUnit unit){

        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入reids
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //Function<ID,R> dbFallback 是一个函数式接口类型的参数，它表示一个可以接受 ID 类型的输入并返回 R 类型输出的函数。    R r  = dbFallback.apply(id);
    //Class<R> type  代表类型 如Shop.class
    //R：代表返回值的数据类型。
    //ID：代表输入参数（通常是某种标识符）的数据类型。
    public <R,ID> R queryWithPassThrough(String KeyPrefix , ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit) {

        String key = KeyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //缓存中有东西
        //是否非空且不为仅包含空白字符的串
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        //缓存查到了  是为空或只包含空白字符但不为null
        if (json != null) {
            //返回错误信息
            return null;
        }

        //shopJson为null的情况  缓存中没有查到东西
        R r  = dbFallback.apply(id);

        if (r == null) {
            //将空值写进reids
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

       this.set(key,r,time,unit);

        return r;
    }

}
