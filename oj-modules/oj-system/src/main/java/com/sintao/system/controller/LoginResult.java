package com.sintao.system.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResult {
    int code; // 0 失败 1 成果
    String msg; // 失败的信息
}
