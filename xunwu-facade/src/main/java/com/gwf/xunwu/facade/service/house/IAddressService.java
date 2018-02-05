package com.gwf.xunwu.facade.service.house;

import com.gwf.xunwu.entity.SupportAddress;
import com.gwf.xunwu.facade.dto.BaiduLbsDTO;
import com.gwf.xunwu.facade.dto.SubwayDTO;
import com.gwf.xunwu.facade.dto.SubwayStationDTO;
import com.gwf.xunwu.facade.dto.SupportAddressDTO;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.search.BaiduMapLocation;

import java.util.List;
import java.util.Map;

/**
 * 地址服务接口
 * @author gaowenfeng
 */
public interface IAddressService {
    /**
     * 获取所有支持的城市列表
     * @return
     */
    ServiceMultiResult<SupportAddressDTO> findAllCities();

    /**
     * 根据英文简写获取具体区域的信息
     * @param cityEnName
     * @param regionEnName
     * @return
     */
    Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName);

    /**
     * 根据城市英文简写获取该城市所有支持的区域信息
     * @param cityName
     * @return
     */
    ServiceMultiResult findAllRegionsByCityName(String cityName);

    /**
     * 获取该城市所有的地铁线路
     * @param cityEnName
     * @return
     */
    List<SubwayDTO> findAllSubwayByCity(String cityEnName);

    /**
     * 获取地铁线路所有的站点
     * @param subwayId
     * @return
     */
    List<SubwayStationDTO> findAllStationBySubway(Long subwayId);

    /**
     * 获取地铁线信息
     * @param subwayId
     * @return
     */
    ServiceResult<SubwayDTO> findSubway(Long subwayId);

    /**
     * 获取地铁站点信息
     * @param stationId
     * @return
     */
    ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId);

    /**
     * 根据城市英文简写获取城市详细信息
     * @param cityEnName
     * @return
     */
    ServiceResult<SupportAddressDTO> findCity(String cityEnName);

    /**
     * 根据城市以及具体位置获取百度地图的经纬度
     * @param city
     * @param address
     * @return
     */
    ServiceResult<BaiduMapLocation> getBaiduMapLocation(String city, String address);

    /**
     * 上传百度LBS数据
     * @param baiduLbsDTO
     * @return
     */
    ServiceResult lbsUpload(BaiduLbsDTO baiduLbsDTO);

    /**
     * 移除百度LBS数据
     * @param houseId
     * @return
     */
    ServiceResult removeLBS(long houseId);



}

