package com.sintao.common.core.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.github.pagehelper.PageInfo;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.TableDataInfo;

import java.util.List;

public class BaseController {

    public R<Void> toR(int rows) {
        return rows > 0 ? R.ok() : R.fail();
    }

    public R<Void> toR(boolean result) {
        return result ? R.ok() : R.fail();
    }

    public TableDataInfo getTableDataInfo(List<?> list) {
        if (CollectionUtil.isEmpty(list)) {
            return TableDataInfo.empty();
        }
        return TableDataInfo.success(list, new PageInfo<>(list).getTotal());
    }
}
