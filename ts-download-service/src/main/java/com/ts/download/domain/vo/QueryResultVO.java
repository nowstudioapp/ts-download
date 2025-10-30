package com.ts.download.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 查询结果VO
 * 
 * @author TS Team
 */
@Data
@ApiModel("查询结果")
public class QueryResultVO {

    @ApiModelProperty("任务类型")
    private String taskType;

    @ApiModelProperty("国家代码")
    private String countryCode;

    @ApiModelProperty("总记录数")
    private Long totalCount;

    @ApiModelProperty("有效记录数（去重后）")
    private Long validCount;

    @ApiModelProperty("查询时间戳")
    private Long queryTime;

    @ApiModelProperty("预计文件大小（KB）")
    private Long estimatedFileSizeKB;

    @ApiModelProperty("支持的下载类型")
    private String[] supportedDownloadTypes;

    public QueryResultVO() {
        this.queryTime = System.currentTimeMillis();
        this.supportedDownloadTypes = new String[]{"txt", "xlsx"};
    }
}
