package com.gwf.xunwu.facade.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 房屋照片数据传输对象
 * @author gaowenfeng
 */
@Data
@ToString
public class HousePictureBO {
    private Long id;

    @JsonProperty(value = "house_id")
    private Long houseId;

    private String path;

    @JsonProperty(value = "cdn_prefix")
    private String cdnPrefix;

    private Integer width;

    private Integer height;

}
