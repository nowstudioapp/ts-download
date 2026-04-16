package com.ts.download.domain.dto;

import lombok.Data;

@Data
public class LogQueryReqDTO {
    private String username;
    private String countryCode;
    private String taskType;
    private String startTime;
    private String endTime;
    private Integer page = 1;
    private Integer pageSize = 20;
}
