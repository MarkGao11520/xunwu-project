package com.gwf.xunwu.facade.bo;

import lombok.Data;
import lombok.ToString;

/**
 * 房屋详情数据传输对象
 * @author gaowenfeng
 */
@Data
@ToString
public class HouseDetailBO {
    private String description;

    private String layoutDesc;

    private String traffic;

    private String roundService;

    private Integer rentWay;

    private Long adminId;

    private String address;

    private Long subwayLineId;

    private Long subwayStationId;

    private String subwayLineName;

    private String subwayStationName;

}
