package com.sintao.common.core.domain.controller;

import com.sintao.common.core.domain.domain.R;

public class BaseController {

    public R<Void> toResult(int rows){
        return rows > 0 ? R.ok() : R.fail();
    }

    public R<Void> toResult(boolean result){
        return result ? R.ok() : R.fail();
    }
}
