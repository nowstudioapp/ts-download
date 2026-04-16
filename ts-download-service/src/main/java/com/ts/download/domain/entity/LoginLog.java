package com.ts.download.domain.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class LoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private Integer status;
    private String message;
    private Date createTime;
}
