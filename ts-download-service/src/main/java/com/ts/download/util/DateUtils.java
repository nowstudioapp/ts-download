package com.ts.download.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类
 * 
 * @author TS Team
 */
public class DateUtils {

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * 获取当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }

    /**
     * 日期增加天数
     */
    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    /**
     * 日期格式化
     */
    public static String parseDateToStr(String format, Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(format).format(date);
    }
}
