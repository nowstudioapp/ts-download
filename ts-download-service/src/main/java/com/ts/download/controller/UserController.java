package com.ts.download.controller;

import com.ts.download.annotation.RequireAdmin;
import com.ts.download.domain.dto.UserAddReqDTO;
import com.ts.download.domain.dto.UserUpdateReqDTO;
import com.ts.download.domain.vo.R;
import com.ts.download.domain.vo.UserVO;
import com.ts.download.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    @ApiOperation("用户列表")
    @RequireAdmin
    public R<List<UserVO>> list() {
        return R.ok(userService.listAll(), "查询成功");
    }

    @PostMapping("/add")
    @ApiOperation("添加用户")
    @RequireAdmin
    public R<Void> add(@RequestBody UserAddReqDTO reqDTO) {
        try {
            userService.addUser(reqDTO);
            return R.ok(null, "添加成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/update")
    @ApiOperation("更新用户")
    @RequireAdmin
    public R<Void> update(@RequestBody UserUpdateReqDTO reqDTO) {
        try {
            userService.updateUser(reqDTO);
            return R.ok(null, "更新成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/delete")
    @ApiOperation("删除用户")
    @RequireAdmin
    public R<Void> delete(@RequestParam Long id) {
        try {
            userService.deleteUser(id);
            return R.ok(null, "删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/changePassword")
    @ApiOperation("修改自己的密码")
    public R<Void> changePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        try {
            UserVO user = (UserVO) request.getAttribute("currentUser");
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            if (oldPassword == null || oldPassword.isEmpty()) {
                return R.fail("请输入旧密码");
            }
            if (newPassword == null || newPassword.length() < 6) {
                return R.fail("新密码长度不能少于6位");
            }
            userService.changePassword(user.getId(), oldPassword, newPassword);
            return R.ok(null, "密码修改成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/resetPassword")
    @ApiOperation("管理员重置用户密码")
    @RequireAdmin
    public R<Void> resetPassword(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("id").toString());
            String newPassword = (String) params.get("password");
            if (newPassword == null || newPassword.length() < 6) {
                return R.fail("新密码长度不能少于6位");
            }
            userService.resetPassword(userId, newPassword);
            return R.ok(null, "密码重置成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
