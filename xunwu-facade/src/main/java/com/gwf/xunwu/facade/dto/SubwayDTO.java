package com.gwf.xunwu.facade.dto;

import lombok.Data;
import lombok.ToString;

/**
 * 地铁DTO
 * @author gaowenfeng
 */
@Data
@ToString
public class SubwayDTO {
    private Long id;
    private String name;
    private String cityEnName;
}
