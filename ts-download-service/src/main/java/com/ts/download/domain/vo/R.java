package com.ts.download.domain.vo;

import lombok.Data;

/**
 * 统一响应结果
 * 
 * @author TS Team
 */
@Data
public class R<T> {

    /** 状态码 */
    private int code;

    /** 消息 */
    private String msg;

    /** 数据 */
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功返回结果
     */
    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    /**
     * 成功返回结果
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    /**
     * 成功返回结果
     */
    public static <T> R<T> ok(T data, String msg) {
        return new R<>(200, msg, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> R<T> fail() {
        return new R<>(500, "操作失败", null);
    }

    /**
     * 失败返回结果
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(500, msg, null);
    }

    /**
     * 失败返回结果
     */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }
}
