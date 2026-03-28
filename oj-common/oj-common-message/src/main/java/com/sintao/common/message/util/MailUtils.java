package com.sintao.common.message.util;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public final class MailUtils {

    private static final String[] CODE_SOURCE = {
            "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
            "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "m", "n", "p",
            "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };

    private MailUtils() {
    }

    public static String achieveCode() {
        List<String> codeList = Arrays.asList(CODE_SOURCE.clone());
        Collections.shuffle(codeList);
        StringBuilder builder = new StringBuilder();
        for (String codeChar : codeList) {
            builder.append(codeChar);
        }
        return builder.substring(3, 8);
    }

    public static void sendMail(
            String host,
            Integer port,
            String username,
            String password,
            String from,
            String subject,
            boolean auth,
            boolean startTls,
            boolean startTlsRequired,
            String sslProtocols,
            String email,
            String code
    ) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        props.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        props.put("mail.smtp.ssl.protocols", sslProtocols);
        props.put("mail.user", username);
        props.put("mail.password", password);

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };

        Session mailSession = Session.getInstance(props, authenticator);
        MimeMessage message = new MimeMessage(mailSession);
        String fromAddress = (from == null || from.isBlank() || !from.contains("@")) ? username : from;

        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject(subject);
        message.setSentDate(new Date());
        message.setContent(
                "尊敬的用户：您好！<br/>您的验证码为：<b>" + code
                        + "</b><br/>有效期为 5 分钟，请勿告知他人。",
                "text/html;charset=UTF-8"
        );

        Transport.send(message);
    }
}
