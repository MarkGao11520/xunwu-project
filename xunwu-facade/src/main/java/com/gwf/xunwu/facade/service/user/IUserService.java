package com.gwf.xunwu.facade.service.user;

import com.gwf.xunwu.entity.User;
import com.gwf.xunwu.facade.dto.UserDTO;
import com.gwf.xunwu.facade.result.ServiceResult;

public interface IUserService {
    User findByUserName(String userName);

    ServiceResult<UserDTO> findById(Long userId);
}
