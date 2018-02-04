package com.gwf.xunwu.facade.bo;

import lombok.Data;
import lombok.ToString;

/**
 * 地铁站DTO
 * @author gaowenfeng
 */
@Data
@ToString
public class SubwayStationBO {
    private Long id;
    private Long subwayId;
    private String name;

}
