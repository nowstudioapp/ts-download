package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

@Data
public class TsWsTaskRecordWSGender {

    /** 手机号码 */
    @Excel(name = "号码")
    private String phone;

    @Excel(name = "WS账号")
    private String uid;

    /** 性别 */
//    @Excel(name = "性别",readConverterExp = "-3=无效,-2=无图片,-1=识别不出,0=女,1=男")
    @Excel(name = "性别",readConverterExp = "-3=无效,-2=无图片,-1=识别不出,0=女,1=男")
    private String sex;

    /** 年龄 */
    @Excel(name = "年龄",cellType = Excel.ColumnType.NUMERIC)
    private Integer age;

    /** 是否商业号 */
    @Excel(name = "是否商业号",readConverterExp = "0=否,1=是")
    private Integer businessNumber;

    /** 是否多人头像 */
    @Excel(name = "头像类型",readConverterExp = "0=单人头像,1=多人头像,-1=其他,-3= ")
    private String multipleAvatars;

    /** 发色 */
    @Excel(name = "发色",readConverterExp = "-1= ,0=黑色,1=金色,2=棕色,3=灰白色")
    private String hairColor;

    /** 肤色 */
    @Excel(name = "肤色",readConverterExp = "-1= ,0=黄色皮肤,1=棕色皮肤,2=黑色皮肤,3=白色皮肤")
    private String skin;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 头像 */
    @Excel(name = "头像")
    private String pic;


}
