package com.sintao.common.core.domain;

import com.sintao.common.core.enums.ResultCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格分页数据对象
 */
public class TableDataInfo {

    private long total;
    private List<?> rows;
    private int code;
    private String msg;

    public TableDataInfo() {
    }

    public static TableDataInfo empty() {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(ResultCode.SUCCESS.getCode());
        rspData.setRows(new ArrayList<>());
        rspData.setMsg(ResultCode.SUCCESS.getMsg());
        rspData.setTotal(0);
        return rspData;
    }

    public static TableDataInfo success(List<?> list, long total) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(ResultCode.SUCCESS.getCode());
        rspData.setRows(list);
        rspData.setMsg(ResultCode.SUCCESS.getMsg());
        rspData.setTotal(total);
        return rspData;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<?> getRows() {
        return rows;
    }

    public void setRows(List<?> rows) {
        this.rows = rows;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
