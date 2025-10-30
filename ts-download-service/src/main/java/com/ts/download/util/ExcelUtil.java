package com.ts.download.util;

import com.ts.download.annotation.Excel;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Excel工具类 - 简化版
 * 
 * @author TS Team
 */
@Slf4j
public class ExcelUtil<T> {

    private Class<T> clazz;
    private List<String> showColumns;

    public ExcelUtil(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 设置要显示的列
     */
    public void showColumn(String... columns) {
        this.showColumns = Arrays.asList(columns);
    }

    /**
     * 导出Excel
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String fileName) {
        try {
            // 创建工作簿
            SXSSFWorkbook workbook = new SXSSFWorkbook(1000);
            SXSSFSheet sheet = (SXSSFSheet) workbook.createSheet(sheetName);
            
            // 启用列宽自动追踪（SXSSF必须）
            sheet.trackAllColumnsForAutoSizing();

            // 获取要导出的字段
            List<Field> fields = getExportFields();

            // 创建表头
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < fields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(getFieldName(fields.get(i)));
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            CellStyle dataStyle = createDataStyle(workbook);
            for (int i = 0; i < list.size(); i++) {
                Row row = sheet.createRow(i + 1);
                T item = list.get(i);
                for (int j = 0; j < fields.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = getFieldValue(item, fields.get(j));
                    setCellValue(cell, value);
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < fields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + 
                java.net.URLEncoder.encode(fileName, "UTF-8"));

            // 写入输出流
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
            workbook.dispose();

        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 获取要导出的字段（只导出带@Excel注解的字段）
     */
    private List<Field> getExportFields() {
        List<Field> result = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        
        // 只导出带@Excel注解的字段
        for (Field field : fields) {
            if (field.isAnnotationPresent(Excel.class)) {
                field.setAccessible(true);
                result.add(field);
            }
        }
        
        return result;
    }

    /**
     * 获取字段名称（从@Excel注解获取）
     */
    private String getFieldName(Field field) {
        Excel excel = field.getAnnotation(Excel.class);
        if (excel != null && !excel.name().isEmpty()) {
            return excel.name();
        }
        // 如果没有注解，使用字段名
        String name = field.getName();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * 获取字段值（支持数据转换）
     */
    private Object getFieldValue(T obj, Field field) {
        try {
            Object value = field.get(obj);
            
            // 处理数据转换
            Excel excel = field.getAnnotation(Excel.class);
            if (excel != null && !excel.readConverterExp().isEmpty() && value != null) {
                return convertByExp(value.toString(), excel.readConverterExp());
            }
            
            return value;
        } catch (IllegalAccessException e) {
            log.error("获取字段值失败", e);
            return null;
        }
    }
    
    /**
     * 数据转换（如：0=男,1=女,2=未知）
     */
    private String convertByExp(String value, String converterExp) {
        try {
            String[] convertSource = converterExp.split(",");
            for (String item : convertSource) {
                String[] itemArray = item.split("=");
                if (itemArray.length == 2 && itemArray[0].equals(value)) {
                    return itemArray[1];
                }
            }
        } catch (Exception e) {
            log.error("数据转换失败", e);
        }
        return value;
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, (Date) value));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
