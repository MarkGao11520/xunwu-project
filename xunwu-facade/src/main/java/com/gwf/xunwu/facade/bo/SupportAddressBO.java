package com.gwf.xunwu.facade.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 地址数据传输对象
 * @author gaowenfeng
 */
@Data
@ToString
public class SupportAddressBO {
    private Long id;
    @JsonProperty(value = "belong_to")
    private String belongTo;

    @JsonProperty(value = "en_name")
    private String enName;

    @JsonProperty(value = "cn_name")
    private String cnName;

    private String level;
}
