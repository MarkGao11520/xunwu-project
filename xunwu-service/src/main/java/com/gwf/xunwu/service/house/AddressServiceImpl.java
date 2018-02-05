package com.gwf.xunwu.service.house;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwf.xunwu.entity.Subway;
import com.gwf.xunwu.entity.SubwayStation;
import com.gwf.xunwu.entity.SupportAddress;
import com.gwf.xunwu.facade.dto.BaiduLbsDTO;
import com.gwf.xunwu.facade.dto.SubwayDTO;
import com.gwf.xunwu.facade.dto.SubwayStationDTO;
import com.gwf.xunwu.facade.dto.SupportAddressDTO;
import com.gwf.xunwu.facade.search.BaiduMapLocation;
import com.gwf.xunwu.facade.service.house.IAddressService;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.repository.SubwayRepository;
import com.gwf.xunwu.repository.SubwayStationRepository;
import com.gwf.xunwu.repository.SupportAddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地区实现类
 * @author gaowenfeng
 */
@Service
@Slf4j
public class AddressServiceImpl implements IAddressService {
    private static final String LOG_PRE = "地区实现服务>";

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${baidu.map.serverKey}")
    private String serverKey;

    @Value("${baidu.map.geoConvApi}")
    private String geoConvApi;

    /**
     * poi 数据管理接口
     */
    @Value("${baidu.lbs.create}")
    private String lbsCreateApi;

    @Value("${baidu.lbs.query}")
    private String lbsQueryApi;

    @Value("${baidu.lbs.update}")
    private String lbsUpdateApi;

    @Value("${baidu.lbs.delete}")
    private String lbsDeleteApi;

    @Value("${baidu.lbs.tableId}")
    private String tableId;


    @Override
    public ServiceMultiResult<SupportAddressDTO> findAllCities() {
        List<SupportAddress> addresses = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        List<SupportAddressDTO> addressDTOS = addresses.stream()
                .map(supportAddress ->  modelMapper.map(supportAddress,SupportAddressDTO.class)).collect(Collectors.toList());
        return new ServiceMultiResult<>(addressDTOS.size(),addressDTOS);
    }

    @Override
    public Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressDTO> result = new HashMap<>(2);

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY
                .getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressDTO.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressDTO.class));
        return result;
    }

    @Override
    public ServiceMultiResult<SupportAddressDTO> findAllRegionsByCityName(String cityName) {
        if (cityName == null) {
            return new ServiceMultiResult<>(0, null);
        }

        List<SupportAddressDTO> result = new ArrayList<>();

        List<SupportAddress> regions = supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION
                .getValue(), cityName);
        for (SupportAddress region : regions) {
            result.add(modelMapper.map(region, SupportAddressDTO.class));
        }
        return new ServiceMultiResult<>(regions.size(), result);
    }

    @Override
    public List<SubwayDTO> findAllSubwayByCity(String cityEnName) {
        List<SubwayDTO> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()) {
            return result;
        }

        subways.forEach(subway -> result.add(modelMapper.map(subway, SubwayDTO.class)));
        return result;
    }

    @Override
    public List<SubwayStationDTO> findAllStationBySubway(Long subwayId) {
        List<SubwayStationDTO> result = new ArrayList<>();
        List<SubwayStation> stations = subwayStationRepository.findAllBySubwayId(subwayId);
        if (stations.isEmpty()) {
            return result;
        }

        stations.forEach(station -> result.add(modelMapper.map(station, SubwayStationDTO.class)));
        return result;
    }

    @Override
    public ServiceResult<SubwayDTO> findSubway(Long subwayId) {
        if (subwayId == null) {
            return ServiceResult.notFound();
        }
        Subway subway = subwayRepository.findOne(subwayId);
        if (subway == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(subway, SubwayDTO.class));
    }

    @Override
    public ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId) {
        if (stationId == null) {
            return ServiceResult.notFound();
        }
        SubwayStation station = subwayStationRepository.findOne(stationId);
        if (station == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(station, SubwayStationDTO.class));
    }

    @Override
    public ServiceResult<SupportAddressDTO> findCity(String cityEnName) {
        if (cityEnName == null) {
            return ServiceResult.notFound();
        }

        SupportAddress supportAddress = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (supportAddress == null) {
            return ServiceResult.notFound();
        }

        SupportAddressDTO addressDTO = modelMapper.map(supportAddress, SupportAddressDTO.class);
        return ServiceResult.of(addressDTO);
    }

    @Override
    public ServiceResult<BaiduMapLocation> getBaiduMapLocation(String city, String address) {
        /**
         * 1.对参数进行urlencode
         * 2.构造http请求体
         * 3.解析http返回参数
         */
        JsonNode jsonNode = httpOperate(()->{
            String encodeCity;
            String encodeAddress;

            try {
                encodeCity = URLEncoder.encode(city,"UTF-8");
                encodeAddress = URLEncoder.encode(address,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error(LOG_PRE+"查询百度地图经纬度异常",e);
                return null;
            }


            StringBuilder sb = new StringBuilder(geoConvApi);
            sb.append("address=").append(encodeAddress).append("&")
                    .append("city=").append(encodeCity).append("&")
                    .append("output=json&")
                    .append("ak=").append(serverKey);

            HttpGet get = new HttpGet(sb.toString());
            return get;
        });
        if(null == jsonNode){
            return new ServiceResult<>(false);
        }

        int status = jsonNode.get("status").asInt();
        if(0!=status){
            return new ServiceResult<>(false,"can not get baidu map location for status:"+status);
        }
        BaiduMapLocation location = new BaiduMapLocation();
        JsonNode jsonLocation = jsonNode.get("result").get("location");
        location.setLatitude(jsonLocation.get("lat").asDouble());
        location.setLongitude(jsonLocation.get("lng").asDouble());
        return ServiceResult.of(location);
    }

    @Override
    public ServiceResult lbsUpload(BaiduLbsDTO baiduLbsDTO) {
        JsonNode jsonNode = httpOperate(()-> {
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("latitude", String.valueOf(baiduLbsDTO.getBaiduMapLocation().getLatitude())));
            nvps.add(new BasicNameValuePair("longitude", String.valueOf(baiduLbsDTO.getBaiduMapLocation().getLongitude())));
            // 百度坐标系
            nvps.add(new BasicNameValuePair("coord_type", "3"));
            nvps.add(new BasicNameValuePair("geotable_id", tableId));
            nvps.add(new BasicNameValuePair("ak", serverKey));
            nvps.add(new BasicNameValuePair("houseId", baiduLbsDTO.getHouseId().toString()));
            nvps.add(new BasicNameValuePair("price", baiduLbsDTO.getPrice().toString()));
            nvps.add(new BasicNameValuePair("area", baiduLbsDTO.getArea().toString()));
            nvps.add(new BasicNameValuePair("title", baiduLbsDTO.getTitle()));
            nvps.add(new BasicNameValuePair("address", baiduLbsDTO.getAddress()));
            HttpPost post;
            if (!isLbsDataExists(baiduLbsDTO.getHouseId())) {
                post = new HttpPost(lbsCreateApi);
            } else {
                post = new HttpPost(lbsUpdateApi);
            }
            try {
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.error(LOG_PRE + "Error to fetch lbs create/upload api", e);
                return null;
            }
            return post;
        });

        return getServiceResult(jsonNode);
    }

    private ServiceResult getServiceResult(JsonNode jsonNode) {
        if(null == jsonNode){
            return new ServiceResult(false);
        }

        int  status = jsonNode.get("status").asInt();
        if (status != 0) {
            String message = jsonNode.get("message").asText();
            log.error(LOG_PRE+"Error to upload lbs data for status: {}, and message: {}", status, message);
            return new ServiceResult(false, "Error to upload lbs data");
        } else {
            return ServiceResult.success();
        }
    }

    @Override
    public ServiceResult removeLBS(long houseId) {
        JsonNode jsonNode = httpOperate(()-> {
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("geotable_id", tableId));
            nvps.add(new BasicNameValuePair("ak", serverKey));
            nvps.add(new BasicNameValuePair("houseId", String.valueOf(houseId)));

            HttpPost delete = new HttpPost(lbsDeleteApi);
            try {
                delete.setEntity(new UrlEncodedFormEntity(nvps,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return null;
            }
            return delete;
        });

        return getServiceResult(jsonNode);

    }

    private boolean isLbsDataExists(Long houseId){

        JsonNode jsonNode = httpOperate(()->{
            StringBuilder sb = new StringBuilder(lbsQueryApi);
            sb.append("geotable_id=").append(tableId).append("&")
                    .append("ak=").append(serverKey).append("&")
                    .append("houseId=").append(houseId).append(",").append(houseId);

            HttpGet get = new HttpGet(sb.toString());
            return get;
        });
        if(null == jsonNode){
            return false;
        }

        int status = jsonNode.get("status").asInt();
        if(0!=status){
            log.error(LOG_PRE+"Error to get lbs data for status: "+status);
            return false;
        }else {
            long size = jsonNode.get("size").asLong();
            if(size > 0){
                return true;
            }else {
                return false;
            }
        }
    }

    private JsonNode httpOperate(HttpOperate httpOperate){
        HttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest request = httpOperate.prepareRequest();
        if(request == null){
            return null;
        }
        try {
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error("Can not get  data for response: " + response);
                return null;
            }
            String result = EntityUtils.toString(response.getEntity(),"UTF-8");
            return objectMapper.readTree(result);
        } catch (IOException e) {
            log.error(LOG_PRE+"Error to fetch api",e);
            return null;
        }
    }


    private interface HttpOperate{
        /**
         * 准备请求参数
         * @return
         */
        HttpUriRequest prepareRequest();
    }
}
