package com.ts.download.excel;

import com.ts.download.annotation.Excel;
import lombok.Data;

import java.util.Date;

@Data
public class TsWsTaskRecordViber {

    @Excel(name = "手机号")
    private String phone;

    @Excel(name = "mid")
    private String userName;

    /** 最后上线时间 */
    @Excel(name = "上次登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date lastOnlineTime;

    @Excel(name = "距离当前天数",cellType = Excel.ColumnType.NUMERIC)
    private Integer activeDay;
}
