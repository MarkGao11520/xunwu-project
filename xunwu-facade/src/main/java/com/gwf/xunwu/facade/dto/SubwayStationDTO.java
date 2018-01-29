package com.gwf.xunwu.facade.dto;

import lombok.Data;
import lombok.ToString;

/**
 * 地铁站DTO
 * @author gaowenfeng
 */
@Data
@ToString
public class SubwayStationDTO {
    private Long id;
    private Long subwayId;
    private String name;

}
