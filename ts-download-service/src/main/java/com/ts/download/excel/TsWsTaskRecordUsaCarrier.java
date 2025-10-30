package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

@Data
public class TsWsTaskRecordUsaCarrier {

    //号码,号码类型,国家简称,运营商, 底层运营商,⾏政区,⾏政区中⽂,城市
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "国家简称")
    private String uid;

    @Excel(name = "运营商")
    private String sex;

    @Excel(name = "底层运营商")
    private String userName;

    @Excel(name = "行政区")
    private String ethnicity;

    @Excel(name = "行政区中文")
    private String hairColor;

    @Excel(name = "城市")
    private String skin;

}
