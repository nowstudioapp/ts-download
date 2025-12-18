package com.ts.download.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合并下载请求DTO（两个任务类型合并）
 * 
 * @author TS Team
 */
@Data
@ApiModel("合并下载请求参数")
public class MergeDownloadReqDTO {

    @ApiModelProperty(value = "国家代码", required = true, example = "US")
    private String countryCode;

    @ApiModelProperty(value = "导出数量（不传或0表示导出全部）", example = "1000")
    private Integer limit;

    @ApiModelProperty(value = "第一个任务类型（默认gender）", example = "gender")
    private String firstTaskType = "gender";

    @ApiModelProperty(value = "第二个任务类型", required = true, example = "rcsValid")
    private String secondTaskType;

    @ApiModelProperty(value = "最小年龄", example = "18")
    private Integer minAge;

    @ApiModelProperty(value = "最大年龄", example = "35")
    private Integer maxAge;

    @ApiModelProperty(value = "性别：0=女，1=男，-1=未知，-2=无图片（只查询性别为空或NULL的记录，不包含未知）", example = "0")
    private Integer sex;

    @ApiModelProperty(value = "排除的肤色：0=黄种人，1=棕种人，2=黑种人，3=白种人（输入2表示不要黑种人的数据）", example = "2")
    private Integer excludeSkin;

    @ApiModelProperty(value = "指定导出的肤色：0=黄种人，1=棕种人，2=黑种人，3=白种人（输入3表示只导出白种人的数据，主要用于TG头像和WS性别）", example = "3")
    private Integer includeSkin;

    @ApiModelProperty(value = "检查user_name是否为空：0=查询user_name为空的，1=查询user_name不为空的，不传则不限制", example = "0")
    private Integer checkUserNameEmpty;

    @ApiModelProperty(value = "跳过记录数量（用于分批下载，不传或0表示从头开始）", example = "100000")
    private Integer skip;

    @ApiModelProperty(value = "下载类型：excel=Excel文件，txt=TXT文件（只含手机号）", example = "excel")
    private String downloadType = "excel";
}
