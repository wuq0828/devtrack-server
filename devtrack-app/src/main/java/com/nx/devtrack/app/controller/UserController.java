package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.common.dto.UserBriefDto;
import com.nx.devtrack.common.web.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 用户查询:供「处理人」下拉与 ID→名字 展示使用。 */
@RestController
@RequestMapping("/devtrack/user")
@RequiredArgsConstructor
public class UserController {

    private final UserDao userDao;

    /** 全部用户(简要)。 */
    @PostMapping("/list")
    public CommonResponse<List<UserBriefDto>> list() {
        List<UserBriefDto> users = userDao.findAll().stream().map(u -> {
            UserBriefDto dto = new UserBriefDto();
            dto.setUserId(u.getId());
            String name = u.getDisplayName();
            dto.setName(name != null && !name.isBlank() ? name : u.getUsername());
            return dto;
        }).toList();
        return CommonResponse.ok(users);
    }
}
