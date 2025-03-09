package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

//拦截器可以在请求到达控制器之前进行权限验证，确保只有授权用户才能访问某些资源。例如，在Spring框架中，可以通过实现 HandlerInterceptor 接口来实现登录验证：
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }
    //ctrl+i
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        System.out.println(token);
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("IP Address: " + request.getRemoteAddr());
        if (StrUtil.isBlank(token)) {
            //Java Web 开发中用于向客户端发送错误响应的一种方式
            //sc：HTTP 状态码，例如 HttpServletResponse.SC_UNAUTHORIZED（401）
            //msg：错误消息，例如 "未授权
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未授权");
            return false;
        }

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_CODE_KEY + token);

        //3.判断用户是否存在
        if (userMap.isEmpty()) {
            //4.不存在，拦截
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未授权");
            return false;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        //5.存在，将用户信息保存在Threadlocal
        UserHolder.saveUser(userDTO);

        stringRedisTemplate.expire(RedisConstants.LOGIN_CODE_KEY + token,LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
