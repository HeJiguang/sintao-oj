package com.sintao.friend.test;

import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/nginx")
@Slf4j
public class NginxTestController  extends BaseController {

    @GetMapping("/info")
    public R<Void> info() {
        log.info("璐熻浇鍧囪　娴嬭瘯");
        return R.ok();
    }

//    location /bitoj-dev/ {
//        proxy_pass http://bitoj/;
//    }

//    upstream bitoj {
//        server 192.168.100.136:9202 weight=1;
//        server 192.168.100.136:19202 weight=2;
//    }

}

