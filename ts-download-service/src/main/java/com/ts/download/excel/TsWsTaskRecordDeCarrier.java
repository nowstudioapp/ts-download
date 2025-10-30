package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

/**
 * 德国运营商
 */
@Data
public class TsWsTaskRecordDeCarrier {

    //号码、号码类型、运营商
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "运营商")
    private String uid;

}
