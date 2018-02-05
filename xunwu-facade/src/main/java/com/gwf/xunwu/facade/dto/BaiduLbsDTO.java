package com.gwf.xunwu.facade.dto;

import com.gwf.xunwu.facade.search.BaiduMapLocation;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author gaowenfeng
 */
@Data
@ToString
@Builder(toBuilder = true)
public class BaiduLbsDTO {
    private BaiduMapLocation baiduMapLocation;
    private String title;
    private String address;
    private Long houseId;
    private Long price;
    private Long area;
}
