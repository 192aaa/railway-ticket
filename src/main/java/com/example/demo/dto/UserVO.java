package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phoneMasked;

    public static UserVO from(User u) {
        if (u == null) return null;
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        if (u.getPhone() != null && u.getPhone().length() >= 7) {
            vo.setPhoneMasked(u.getPhone().substring(0, 3) + "****" + u.getPhone().substring(u.getPhone().length() - 4));
        } else {
            vo.setPhoneMasked(u.getPhone());
        }
        return vo;
    }
}