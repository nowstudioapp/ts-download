package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

import java.util.Date;

@Data
public class TsWsTaskRecordTG {

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phone;

    @Excel(name = "UID")
    private String uid;

    @Excel(name = "userName")
    private String userName;

    /** 头像 */
    @Excel(name = "头像")
    private String pic;

    @Excel(name = "是否会员")
    private String member;

    /** 最后上线时间 */
    @Excel(name = "最后上线时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastOnlineTime;

    /** 最后上线时间 */
    @Excel(name = "最后上线时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private String lastOnlineTimeStr;

    @Excel(name = "有效天数",cellType = Excel.ColumnType.NUMERIC)
    private Integer activeDay;

    /** 性别 */
    @Excel(name = "性别")
    private String sex;

    /** 年龄 */
    @Excel(name = "年龄",cellType = Excel.ColumnType.NUMERIC)
    private Integer age;


//    @Excel(name = "人种",readConverterExp = "Asia=亚洲,Europe=欧洲,North America=北美洲,South America=南美洲,Africa=非洲")
    @Excel(name = "人种")
    private String ethnicity;

    @Excel(name = "first_name")
    private String firstName;

    @Excel(name = "last_name")
    private String lastName;

    @Excel(name = "国家")
    private String country;

    @Excel(name = "地区")
    private String region;
}
