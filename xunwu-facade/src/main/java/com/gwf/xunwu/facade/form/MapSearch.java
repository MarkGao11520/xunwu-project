package com.gwf.xunwu.facade.form;

import lombok.Data;
import lombok.ToString;

/**
 * Created by 瓦力.
 */
@Data
@ToString
public class MapSearch {
    private String cityEnName;

    /**
     * 地图缩放级别
     */
    private int level = 12;
    private String orderBy = "lastUpdateTime";
    private String orderDirection = "desc";
    /**
     * 左上角
     */
    private Double leftLongitude;
    private Double leftLatitude;

    /**
     * 右下角
     */
    private Double rightLongitude;
    private Double rightLatitude;

    private int start = 0;
    private int size = 5;

}
