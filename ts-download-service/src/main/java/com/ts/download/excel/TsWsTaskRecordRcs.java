package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

@Data
public class TsWsTaskRecordRcs {

    @Excel(name = "手机号")
    private String phone;

    @Excel(name = "系统")
    private String userName;
}
