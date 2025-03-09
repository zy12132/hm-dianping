package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String sendCode(String phone, HttpSession session) {


        //3.符合，生成验证码
        // Hutool 工具库   返回一个长度为 6 的随机数字字符串
        String code = RandomUtil.randomNumbers(6);

        //4.保存验证码到session
        //session.setAttribute("code", code); 是 Java Web 开发中用于将验证码存储到 Session 中的方法。它的作用是将生成的验证码与当前用户的会话关联起来，以便在后续的请求中进行验证

        //存到redis中
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code,LOGIN_CODE_TTL, TimeUnit.MINUTES);


//        session.setAttribute("code", code);

        //返回ok
        log.debug("短信验证码已经发送，{}", code);
        return code;
    }

    //验证code
    @Override
    public Result varCode(LoginFormDTO loginForm, HttpSession session) {


        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());


        // 1. Get the verification code from the session
//        String storedCode = (String) session.getAttribute("code");

        // 2. Get the verification code from the login form
        String userCode = loginForm.getCode();

        // 3. Compare the codes
        if (code != null && code.equals(userCode)) {
            User user = query().eq("phone", loginForm.getPhone()).one();

            if (user == null) {
                user = createUserWithPhone(loginForm.getPhone());
            }

            String token = UUID.randomUUID().toString(true);

            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

            Map<String, Object> userMap = BeanUtil.beanToMap(
                    userDTO,
                    new HashMap<>(),
                    CopyOptions.create()
                            .setFieldValueEditor((fieldName, fieldValue) -> String.valueOf(fieldValue))
            );
            String tokenKey = LOGIN_USER_KEY + token;

            stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
//设置token有效期
            stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);

            return Result.ok(token); // Codes match
        }

        // 4. Codes do not match or session code is null
        return null;
    }

    private User createUserWithPhone(String phone) {
        //1.创建信息
        User user = User.builder()
                .phone(phone)
                .nickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10)).build();

        //2.保存用户
        save(user);
        return user;
    }
}
