package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

/**
 * 西班牙运营商
 */
@Data
public class TsWsTaskRecordEsCarrier {

    //号码,号码类型,运营商,省份,省份中⽂
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "号码类型")
    private String member;

    @Excel(name = "运营商")
    private String uid;

    @Excel(name = "省份")
    private String userName;

    @Excel(name = "省份中⽂")
    private String ethnicity;


}
