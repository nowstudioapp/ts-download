package com.ts.download.domain.entity;

import lombok.Data;
import java.util.Date;

/**
 * 任务记录实体类
 * 
 * @author TS Team
 */
@Data
public class TsWsTaskRecord {

    /** 手机号 */
    private String phone;

    /** 用户ID */
    private String uid;

    /** 用户名 */
    private String userName;

    /** 头像 */
    private String pic;

    /** 会员状态 */
    private String member;

    /** 最后在线时间 */
    private Date lastOnlineTime;

    /** 最后在线时间字符串 */
    private String lastOnlineTimeStr;

    /** 活跃天数 */
    private Integer activeDay;

    /** 状态 */
    private String status;

    /** 性别 */
    private String sex;

    /** 年龄 */
    private Integer age;

    /** 是否商业号 */
    private Integer businessNumber;

    /** 种族 */
    private String ethnicity;

    /** 名字 */
    private String firstName;

    /** 姓氏 */
    private String lastName;

    /** 国家 */
    private String country;

    /** 地区 */
    private String region;

    /** 发色 */
    private String hairColor;

    /** 肤色 */
    private String skin;

    /** 多头像 */
    private String multipleAvatars;
    
    /** 创建时间 */
    private Date createTime;
}
