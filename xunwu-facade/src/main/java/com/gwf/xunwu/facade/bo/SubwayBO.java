package com.gwf.xunwu.facade.bo;

import lombok.Data;
import lombok.ToString;

/**
 * 地铁DTO
 * @author gaowenfeng
 */
@Data
@ToString
public class SubwayBO {
    private Long id;
    private String name;
    private String cityEnName;
}
