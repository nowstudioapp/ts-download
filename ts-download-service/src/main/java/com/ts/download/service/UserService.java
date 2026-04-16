package com.ts.download.service;

import com.ts.download.domain.dto.UserAddReqDTO;
import com.ts.download.domain.dto.UserUpdateReqDTO;
import com.ts.download.domain.vo.UserVO;

import java.util.List;

public interface UserService {
    List<UserVO> listAll();
    void addUser(UserAddReqDTO reqDTO);
    void updateUser(UserUpdateReqDTO reqDTO);
    void deleteUser(Long id);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void resetPassword(Long userId, String newPassword);
}
