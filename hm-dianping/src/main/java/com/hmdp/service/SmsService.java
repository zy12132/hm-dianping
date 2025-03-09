package com.hmdp.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    public  void sendSms(String phone, String code) throws Exception {
        Config config = new Config()
                //阿里云 SDK 中的一个配置类，用于设置访问阿里云服务所需的参数
                .setAccessKeyId("LTAI5tQiDDV6GFjCpFXA3213") // 替换为你的 AccessKey ID
                .setAccessKeySecret("oTetLiWUkOgY5FjKYj5WW8247hrdwV"); // 替换为你的 AccessKey Secret
        config.endpoint = "dysmsapi.aliyuncs.com";// 设置短信服务的 Endpoint


        // // 使用 Config 创建阿里云短信客户端
        Client client = new Client(config);
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName("阿伟") // 替换为你的签名名称
                .setTemplateCode("SMS_479030597") // 替换为你的模板 ID
                .setTemplateParam("{\"code\":\"" + code + "\"}");

        SendSmsResponse response = client.sendSms(request);
        System.out.println("短信发送结果：" + response.getBody().getMessage());
    }
}