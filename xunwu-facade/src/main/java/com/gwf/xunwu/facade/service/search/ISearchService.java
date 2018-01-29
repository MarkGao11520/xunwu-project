package com.gwf.xunwu.facade.service.search;

/**
 * es查询服务接口
 * @author gaowenfeng
 */
public interface ISearchService {

    /**
     * 索引目标房源
     * @param houseId 房屋id
     */
    void index(Long houseId);

    /**
     * 移除房源索引
     * @param houseId 房屋id
     */
    void remove(Long houseId);
}
