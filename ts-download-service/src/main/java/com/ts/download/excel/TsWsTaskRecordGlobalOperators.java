package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

@Data
public class TsWsTaskRecordGlobalOperators {

    @Excel(name = "目标")
    private String phone;

    @Excel(name = "有效性")
    private String member;

    @Excel(name = "完整号码")
    private String uid;

    @Excel(name = "本地格式")
    private String sex;

    @Excel(name = "国际格式")
    private String userName;

    @Excel(name = "国家电话区号")
    private String ethnicity;

    @Excel(name = "国家代码")
    private String hairColor;

    @Excel(name = "国家名称")
    private String skin;

    @Excel(name = "归属地")
    private String firstName;

    @Excel(name = "所属运营商")
    private String lastName;

    @Excel(name = "线路类型")
    private String multipleAvatars;
}
