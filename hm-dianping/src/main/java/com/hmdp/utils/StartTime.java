package com.hmdp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class StartTime{

    public final Logger logger = LoggerFactory.getLogger(RedisData.class);
    public Long time() {
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        logger.info("second:{}",second);
        return second;
    }
}
