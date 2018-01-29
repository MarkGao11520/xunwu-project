package com.gwf.xunwu.facade.base;

/**
 * 房屋操作状态常量定义
 * @author gaowenfeng
 */
public interface HouseOperation {
    /** 通过审核 */
    int PASS = 1;

    /** 下架。重新审核 */
    int PULL_OUT = 2;

    /** 逻辑删除 */
    int DELETE = 3;

    /** 出租 */
    int RENT = 4;
}
