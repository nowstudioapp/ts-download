package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

/**
 * 越南运营商
 */
@Data
public class TsWsTaskRecordVnCarrier {

    //号码、号码类型、运营商、底层运营商、城市中文、城市
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "运营商")
    private String uid;

    @Excel(name = "底层运营商")
    private String userName;

    @Excel(name = "城市中文")
    private String ethnicity;

    @Excel(name = "城市")
    private String hairColor;

}
