package com.gwf.xunwu.service.user;

import com.gwf.xunwu.entity.Role;
import com.gwf.xunwu.entity.User;
import com.gwf.xunwu.facade.bo.UserBO;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.service.user.IUserService;
import com.gwf.xunwu.repository.RoleRepository;
import com.gwf.xunwu.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现类
 * @author gaowenfeng
 */
@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public User findByUserName(String userName) {
        User user = userRepository.findByName(userName);

        if(null == user){
            return null;
        }

        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        if(CollectionUtils.isEmpty(roles)){
            throw new DisabledException("权限非法");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" +role.getName())));
        user.setAuthorityList(authorities);
        return user;
    }

    @Override
    public ServiceResult<UserBO> findById(Long userId) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            return ServiceResult.notFound();
        }
        UserBO userDTO = modelMapper.map(user, UserBO.class);
        return ServiceResult.of(userDTO);
    }
}
