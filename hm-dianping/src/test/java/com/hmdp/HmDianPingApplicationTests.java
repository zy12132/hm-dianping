package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testConnection() {
        redisTemplate.opsForValue().set("test", "Hello, Redis!");
        String value = redisTemplate.opsForValue().get("test");
        System.out.println("Redis Test: " + value);
    }


    @Autowired
    public RedisIdWorker redisIdWorker;

    public ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Test
    void rediswork() throws InterruptedException {

        //CountDownLatch 是 Java 并发包中的一个同步工具类，用于协调多个线程的执行。
        //这里创建了一个初始计数为 300 的 CountDownLatch 对象，意味着需要等待 300 个任务完成。
        CountDownLatch latch = new CountDownLatch(300);

        //Runable函数式接口
        //() -> { ... } 是一个 Lambda 表达式，
        //它实现了 Runnable 接口的 run() 方法。() 表示 run() 方法没有参数，{ ... } 中包含了 run() 方法的具体实现代码
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id:" + id);
            }
            //latch 是一个 CountDownLatch 对象，用于协调多个线程的执行。
            //countDown() 方法会将 CountDownLatch 的计数减 1，表示当前任务已经完成。当计数减为 0 时，等待在 latch.await() 处的线程将被唤醒。
            latch.countDown();
        };


        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            //executorService 是一个线程池对象，用于管理和执行线程任务
            executorService.submit(task);
        }

        //latch.await() 会阻塞当前线程，直到 CountDownLatch 的计数变为 0，即所有任务都完成。
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time:"+(end - begin));
    }
}
