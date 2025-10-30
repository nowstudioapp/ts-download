package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

/**
 * 乌克兰运营商
 */
@Data
public class TsWsTaskRecordUaCarrier {

    //号码,号码类型,运营商,运营商英⽂,⾏政区,⾏政区中⽂,城市
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "运营商")
    private String uid;

    @Excel(name = "运营商英⽂")
    private String userName;

    @Excel(name = "行政区")
    private String ethnicity;

    @Excel(name = "⾏政区中⽂")
    private String hairColor;

    @Excel(name = "城市")
    private String skin;

}
