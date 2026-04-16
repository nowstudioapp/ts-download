package com.ts.download.domain.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class DownloadLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String countryCode;
    private String firstTaskType;
    private String secondTaskType;
    private String downloadParams;
    private String fileUrl;
    private Date createTime;
}
