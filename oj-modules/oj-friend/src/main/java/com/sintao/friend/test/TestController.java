package com.sintao.friend.test;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import com.sintao.common.message.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController extends BaseController {

    @Autowired
    private MailService mailService;

    @GetMapping("/sendCode")
    public R<Void> sendCode(String email, String code) {
        log.info("mail send code test");
        return toR(mailService.sendLoginCode(email, code));
    }
}
