package com.gwf.xunwu.facade.bo;

import lombok.Data;
import lombok.ToString;

/**
 * 用户数据传输对象
 * @author gaowenfeng
 */
@Data
@ToString
public class UserBO {
    private Long id;
    private String name;
    private String avatar;
    private String phoneNumber;
    private String lastLoginTime;

}
