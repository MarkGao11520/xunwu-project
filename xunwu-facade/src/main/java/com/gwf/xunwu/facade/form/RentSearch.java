package com.gwf.xunwu.facade.form;

import lombok.Data;
import lombok.ToString;

/**
 * 租房请求参数结构体
 * @author gaowenfeng
 */
@Data
@ToString
public class RentSearch {
    public static final String regionAll = "*";

    private String cityEnName;
    private String regionEnName;
    private String priceBlock;
    private String areaBlock;
    private int room;
    private int direction;
    private String keywords;
    private int rentWay = -1;
    private String orderBy = "lastUpdateTime";
    private String orderDirection = "desc";


    private int start = 0;

    private int size = 5;


    public int getStart() {
        return start > 0 ? start : 0;
    }


    public int getSize() {
        int theThresholdValue = 100;
        if (this.size < 1) {
            return 5;
        } else if (this.size > theThresholdValue) {
            return 100;
        } else {
            return this.size;
        }
    }


    public int getRentWay() {
        int min = -2;
        int max = 2;
        if (rentWay > min && rentWay < max) {
            return rentWay;
        } else {
            return -1;
        }
    }

}
