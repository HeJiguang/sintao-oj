package com.sintao.common.core.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageQueryDTO {

    private Integer pageSize = 10;  //每页的数�? 必传

    private Integer pageNum = 1;   //第几�?  必传
}

