package com.ts.download.domain.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
    private Date createTime;
}
