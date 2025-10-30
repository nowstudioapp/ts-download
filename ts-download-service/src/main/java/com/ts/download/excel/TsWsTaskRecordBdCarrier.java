package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

/**
 * 孟加拉运营商
 */
@Data
public class TsWsTaskRecordBdCarrier {

    //号码、号码类型、运营商、城市
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "运营商")
    private String uid;

    @Excel(name = "城市")
    private String userName;

}
