package com.sintao.common.message.service;

import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AliSmsService {

    @Autowired
    private Client aliClient;

    //业务配置
    @Value("${sms.aliyun.templateCode:}")
    private String templateCode;

    @Value("${sms.aliyun.sing-name:}")
    private String singName;

    /**
     * 发送短信验证码
     *
     * @param phone
     * @param code
     */
    public boolean sendMobileCode(String phone, String code) {
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        return sendTempMessage(phone, singName, templateCode, params);
    }

    /**
     * 发送模板消�?     *
     * @param phone
     * @param singName
     * @param templateCode
     * @param params
     */
    public boolean sendTempMessage(String phone, String singName, String templateCode,
                                   Map<String, String> params) {
        SendSmsRequest sendSmsRequest = new SendSmsRequest();
        sendSmsRequest.setPhoneNumbers(phone);
        sendSmsRequest.setSignName(singName);
        sendSmsRequest.setTemplateCode(templateCode);
        sendSmsRequest.setTemplateParam(JSON.toJSONString(params));
        try {
            SendSmsResponse sendSmsResponse = aliClient.sendSms(sendSmsRequest);
            SendSmsResponseBody responseBody = sendSmsResponse.getBody();
            if (!"OK".equalsIgnoreCase(responseBody.getCode())) {
                log.error("短信{} 发送失败，失败原因:{}.... ", JSON.toJSONString(sendSmsRequest), responseBody.getMessage());
                return false;
            }
            return true;
        }  catch (Exception e) {
            log.error("短信{} 发送失败，失败原因:{}.... ",  JSON.toJSONString(sendSmsRequest), e.getMessage());
            return false;
        }
    }
}
