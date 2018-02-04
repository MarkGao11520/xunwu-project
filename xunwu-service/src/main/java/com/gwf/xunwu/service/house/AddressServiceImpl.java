package com.gwf.xunwu.service.house;

import com.gwf.xunwu.entity.Subway;
import com.gwf.xunwu.entity.SubwayStation;
import com.gwf.xunwu.entity.SupportAddress;
import com.gwf.xunwu.facade.bo.SubwayBO;
import com.gwf.xunwu.facade.bo.SubwayStationBO;
import com.gwf.xunwu.facade.bo.SupportAddressBO;
import com.gwf.xunwu.facade.service.house.IAddressService;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.repository.SubwayRepository;
import com.gwf.xunwu.repository.SubwayStationRepository;
import com.gwf.xunwu.repository.SupportAddressRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class AddressServiceImpl implements IAddressService {
    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ServiceMultiResult<SupportAddressBO> findAllCities() {
        List<SupportAddress> addresses = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        List<SupportAddressBO> addressDTOS = addresses.stream()
                .map(supportAddress ->  modelMapper.map(supportAddress,SupportAddressBO.class)).collect(Collectors.toList());
        return new ServiceMultiResult<>(addressDTOS.size(),addressDTOS);
    }

    @Override
    public Map<SupportAddress.Level, SupportAddressBO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressBO> result = new HashMap<>(2);

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY
                .getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressBO.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressBO.class));
        return result;
    }

    @Override
    public ServiceMultiResult<SupportAddressBO> findAllRegionsByCityName(String cityName) {
        if (cityName == null) {
            return new ServiceMultiResult<>(0, null);
        }

        List<SupportAddressBO> result = new ArrayList<>();

        List<SupportAddress> regions = supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION
                .getValue(), cityName);
        for (SupportAddress region : regions) {
            result.add(modelMapper.map(region, SupportAddressBO.class));
        }
        return new ServiceMultiResult<>(regions.size(), result);
    }

    @Override
    public List<SubwayBO> findAllSubwayByCity(String cityEnName) {
        List<SubwayBO> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()) {
            return result;
        }

        subways.forEach(subway -> result.add(modelMapper.map(subway, SubwayBO.class)));
        return result;
    }

    @Override
    public List<SubwayStationBO> findAllStationBySubway(Long subwayId) {
        List<SubwayStationBO> result = new ArrayList<>();
        List<SubwayStation> stations = subwayStationRepository.findAllBySubwayId(subwayId);
        if (stations.isEmpty()) {
            return result;
        }

        stations.forEach(station -> result.add(modelMapper.map(station, SubwayStationBO.class)));
        return result;
    }

    @Override
    public ServiceResult<SubwayBO> findSubway(Long subwayId) {
        if (subwayId == null) {
            return ServiceResult.notFound();
        }
        Subway subway = subwayRepository.findOne(subwayId);
        if (subway == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(subway, SubwayBO.class));
    }

    @Override
    public ServiceResult<SubwayStationBO> findSubwayStation(Long stationId) {
        if (stationId == null) {
            return ServiceResult.notFound();
        }
        SubwayStation station = subwayStationRepository.findOne(stationId);
        if (station == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(station, SubwayStationBO.class));
    }

    @Override
    public ServiceResult<SupportAddressBO> findCity(String cityEnName) {
        if (cityEnName == null) {
            return ServiceResult.notFound();
        }

        SupportAddress supportAddress = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (supportAddress == null) {
            return ServiceResult.notFound();
        }

        SupportAddressBO addressDTO = modelMapper.map(supportAddress, SupportAddressBO.class);
        return ServiceResult.of(addressDTO);
    }
}
