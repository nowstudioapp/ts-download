package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

/**
 * 哈萨克斯坦运营商
 */
@Data
public class TsWsTaskRecordKzCarrier {

    //号码,号码类型,运营商,地区,地区中⽂,城市
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "运营商")
    private String uid;

    @Excel(name = "地区")
    private String userName;

    @Excel(name = "地区中⽂")
    private String ethnicity;

    @Excel(name = "城市")
    private String hairColor;

}
