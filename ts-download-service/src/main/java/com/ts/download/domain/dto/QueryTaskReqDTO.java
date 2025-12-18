package com.ts.download.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 查询任务请求DTO（单个任务类型查询）
 * 
 * @author TS Team
 */
@Data
@ApiModel("查询任务请求参数")
public class QueryTaskReqDTO {

    @ApiModelProperty(value = "任务类型", required = true, example = "gender")
    private String taskType;

    @ApiModelProperty(value = "国家代码", required = true, example = "US")
    private String countryCode;

    @ApiModelProperty(value = "最小年龄", example = "18")
    private Integer minAge;

    @ApiModelProperty(value = "最大年龄", example = "35")
    private Integer maxAge;

    @ApiModelProperty(value = "性别：0=女，1=男，-1=未知，-2=无图片（只查询性别为空或NULL的记录，不包含未知）", example = "0")
    private Integer sex;

    @ApiModelProperty(value = "排除的肤色：0=黄种人，1=棕种人，2=黑种人，3=白种人（支持多选，例如[2,3]表示排除黑种人和白种人）", example = "[2]")
    private List<Integer> excludeSkin;

    @ApiModelProperty(value = "指定查询的肤色：0=黄种人，1=棕种人，2=黑种人，3=白种人（支持多选，例如[0,1]表示只查询黄种人和棕种人，主要用于TG头像和WS性别）", example = "[3]")
    private List<Integer> includeSkin;

    @ApiModelProperty(value = "检查user_name是否为空：0=查询user_name为空的，1=查询user_name不为空的，不传则不限制", example = "0")
    private Integer checkUserNameEmpty;
}
