package com.gwf.xunwu.facade.service.search;

import com.gwf.xunwu.facade.exception.EsException;
import com.gwf.xunwu.facade.form.MapSearch;
import com.gwf.xunwu.facade.form.RentSearch;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.search.BaiduMapLocation;
import com.gwf.xunwu.facade.search.HouseBucketBO;

import java.util.List;

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

    /**
     * 查询
     * @param search
     * @return
     * @throws EsException
     */
    ServiceMultiResult<Long> query(RentSearch search) throws EsException;

    /**
     * 实现自动补全关键词
     * @param prefix
     * @return
     */
    ServiceResult<List<String>> suggest(String prefix);

    /**
     * 聚合特定小区的房间数
     * @param cityEnName 城市名
     * @param regionEnName 区域名
     * @param district 小区名
     * @return
     */
    ServiceResult<Long> aggregateDistrictHouse(String cityEnName,String regionEnName,String district);


    /**
     * 聚合城市数据
     * @param cityEnName
     * @return
     */
    ServiceMultiResult<HouseBucketBO> mapAggregate(String cityEnName);

    /**
     * 城市级别查询
     * @param cityEnName
     * @param orderBy
     * @param orderDirection
     * @param start
     * @param size
     * @return
     */
    ServiceMultiResult<Long> mapQuery(String cityEnName,String orderBy,String orderDirection,int start,int size);

    /**
     * 精确范围查询
     * @param mapSearch
     * @return
     */
    ServiceMultiResult<Long> mapQuery(MapSearch mapSearch);
}
