package com.gwf.xunwu.facade.dto;

import lombok.Data;
import lombok.ToString;

/**
 * Created by 瓦力.
 */
@Data
@ToString
public class UserDTO {
    private Long id;
    private String name;
    private String avatar;
    private String phoneNumber;
    private String lastLoginTime;

}
