package com.gwf.xunwu.facade.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 房屋状态
 * @author gaowenfeng
 */
@Getter
@AllArgsConstructor
public enum  HouseStatus {
    /** 未审核 */
    NOT_AUDITED(0),
    /** 审核通过 */
    PASSES(1),
    /** 已出租 */
    RENTED(2),
    /** 逻辑删除 */
    DELETED(3);

    private int value;

}
