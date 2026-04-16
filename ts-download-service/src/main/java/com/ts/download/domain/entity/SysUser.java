package com.ts.download.domain.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private Integer status;
    private Integer deleted;
    private Date createTime;
    private Date updateTime;
    private Long version;
}
