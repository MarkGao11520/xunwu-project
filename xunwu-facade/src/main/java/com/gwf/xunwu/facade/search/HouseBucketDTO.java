package com.gwf.xunwu.facade.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * 聚合城市数据bucket
 * @author gaowenfeng
 */
@Data
@ToString
@AllArgsConstructor
public class HouseBucketDTO {
    /**
     * 聚合bucket的key
     */
    private String key;

    /**
     * 聚合结果值
     */
    private Long count;
}
