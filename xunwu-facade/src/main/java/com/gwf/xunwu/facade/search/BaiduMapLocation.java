package com.gwf.xunwu.facade.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 百度位置信息
 * @author gaowenfeng
 */
@Data
public class BaiduMapLocation {
    /**
     经度
      */
    @JsonProperty("lon")
    private Double longitude;

    /**
     纬度
      */
    @JsonProperty("lat")
    private Double latitude;

}

