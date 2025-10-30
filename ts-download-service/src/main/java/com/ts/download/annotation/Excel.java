package com.ts.download.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel注解
 * 
 * @author TS Team
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Excel {
    
    /**
     * Excel列名
     */
    String name() default "";
    
    /**
     * 读取内容转换表达式 (如: 0=男,1=女,2=未知)
     */
    String readConverterExp() default "";
    
    /**
     * 列类型（0数字 1字符串）
     */
    ColumnType cellType() default ColumnType.STRING;
    
    /**
     * 列宽度
     */
    int width() default 16;
    
    /**
     * 日期格式
     */
    String dateFormat() default "";
    
    /**
     * 导出类型（0导出导入 1仅导出 2仅导入）
     */
    Type type() default Type.ALL;
    
    /**
     * 列类型枚举
     */
    enum ColumnType {
        NUMERIC, STRING, IMAGE
    }
    
    /**
     * 类型枚举
     */
    enum Type {
        ALL, EXPORT, IMPORT
    }
}
