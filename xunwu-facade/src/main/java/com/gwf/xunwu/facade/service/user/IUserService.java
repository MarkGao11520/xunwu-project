package com.gwf.xunwu.facade.service.user;

import com.gwf.xunwu.entity.User;
import com.gwf.xunwu.facade.dto.UserDTO;
import com.gwf.xunwu.facade.result.ServiceResult;

/**
 * @author gaowenfeng
 */
public interface IUserService {
    /**
     * 根据用户名查找用户信息
     * @param userName
     * @return
     */
    User findByUserName(String userName);

    /**
     * 根据用户id查找用户信息
     * @param userId
     * @return
     */
    ServiceResult<UserDTO> findById(Long userId);
}
