package com.sintao.common.message.service;

import com.sintao.common.message.util.MailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;

@Component
@Slf4j
public class MailService {

    @Value("${mail.host:}")
    private String host;

    @Value("${mail.port:587}")
    private Integer port;

    @Value("${mail.username:}")
    private String username;

    @Value("${mail.password:}")
    private String password;

    @Value("${mail.from:}")
    private String from;

    @Value("${mail.subject:OnlineOJ 邮箱验证码}")
    private String subject;

    @Value("${mail.auth:true}")
    private boolean auth;

    @Value("${mail.starttls:true}")
    private boolean startTls;

    @Value("${mail.starttls-required:true}")
    private boolean startTlsRequired;

    @Value("${mail.ssl-protocols:TLSv1.2}")
    private String sslProtocols;

    public String generateCode() {
        return MailUtils.achieveCode();
    }

    public boolean sendLoginCode(String email, String code) {
        try {
            MailUtils.sendMail(
                    host,
                    port,
                    username,
                    password,
                    from,
                    subject,
                    auth,
                    startTls,
                    startTlsRequired,
                    sslProtocols,
                    email,
                    code
            );
            return true;
        } catch (MessagingException ex) {
            log.error("send mail failed, email={}", email, ex);
            return false;
        }
    }
}
