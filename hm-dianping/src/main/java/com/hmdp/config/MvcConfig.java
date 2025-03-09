package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

//    @Resource
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        //添加拦截器 并排除某些不需要拦截的路径
//        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
//                .excludePathPatterns(
//                        "/user/code",
//                        "/user/login",
//                        "/shop/**",
//                        "/blog/hot",
//                        "/voucher/**",
//                        "/shop-type/**",
//                        "/upload/**",
//                        "/v3/**",
//                        "/swagger-ui/**"
//                );
//    }
}
