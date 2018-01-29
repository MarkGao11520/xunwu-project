package com.gwf.xunwu.utils;

import com.gwf.xunwu.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录用户信息工具类
 * @author gaowenfeng
 */
public class LoginUserUtil {
    public static User load(){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (null != principal && principal instanceof User){
            return (User) principal;
        }

        return null;
    }

    public static Long getLoginUserId(){
        User user = load();
        if(null == user){
            return -1L;
        }
        return user.getId();
    }
}
