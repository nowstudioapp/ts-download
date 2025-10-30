package com.ts.download.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 下载请求DTO
 * 
 * @author TS Team
 */
@Data
@ApiModel("下载请求参数")
public class DownloadReqDTO {

    @ApiModelProperty(value = "下载类型：txt 或 xlsx", required = true, example = "txt")
    private String downloadType;

    @ApiModelProperty(value = "任务类型", required = true, example = "wsValid")
    private String taskType;

    @ApiModelProperty(value = "国家代码", required = true, example = "US")
    private String countryCode;

    @ApiModelProperty(value = "导出数量（不传或0表示导出全部）", example = "1000")
    private Integer limit;
}
