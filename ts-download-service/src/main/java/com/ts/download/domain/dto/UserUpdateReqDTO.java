package com.ts.download.domain.dto;

import lombok.Data;

@Data
public class UserUpdateReqDTO {
    private Long id;
    private String nickname;
    private String password;
    private String role;
    private Integer status;
}
