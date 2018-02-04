package com.gwf.xunwu.facade.search;

import lombok.Data;

/**
 * 关键词
 * @author gaowenfeng
 */
@Data
public class HouseSuggest {
    private String input;
    private int weight = 10;
}
