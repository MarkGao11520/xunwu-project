package com.gwf.xunwu.repository;

import com.gwf.xunwu.ApplicationTest;
import com.gwf.xunwu.entity.User;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserRepositoryTest extends ApplicationTest{
    @Autowired
    private UserRepository userRepository;

    @Test
    public void findOne(){
        User user = userRepository.findOne(1L);
        Assert.assertEquals("gwf", user.getName());
    }
}
