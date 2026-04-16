package com.ts.download.domain.dto;

import lombok.Data;

@Data
public class UserAddReqDTO {
    private String username;
    private String password;
    private String nickname;
    private String role;
}
