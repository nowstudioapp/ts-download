package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

@Data
public class TsWsTaskRecordExists {

    /** 手机号码 */
    @Excel(name = "号码")
    private String phone;

//    @Excel(name = "WS账号")
//    private String uid;

    /** 是否商业号 */
//    @Excel(name = "是否商业号",readConverterExp = "0=否,1=是")
//    private Integer businessNumber;

    /** 状态 */
//    @Excel(name = "签名")
//    private String status;
}
