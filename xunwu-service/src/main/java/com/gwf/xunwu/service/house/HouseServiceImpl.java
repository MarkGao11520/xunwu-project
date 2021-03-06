package com.gwf.xunwu.service.house;

import com.google.common.collect.Maps;
import com.gwf.xunwu.entity.*;
import com.gwf.xunwu.facade.base.HouseSort;
import com.gwf.xunwu.facade.base.HouseStatus;
import com.gwf.xunwu.facade.base.HouseSubscribeStatus;
import com.gwf.xunwu.facade.dto.HouseDTO;
import com.gwf.xunwu.facade.dto.HouseDetailDTO;
import com.gwf.xunwu.facade.dto.HousePictureDTO;
import com.gwf.xunwu.facade.dto.HouseSubscribeDTO;
import com.gwf.xunwu.facade.exception.EsException;
import com.gwf.xunwu.facade.form.*;
import com.gwf.xunwu.facade.service.house.IHouseService;
import com.gwf.xunwu.facade.service.house.IQiNiuService;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.service.search.ISearchService;
import com.gwf.xunwu.repository.*;
import com.gwf.xunwu.utils.LoginUserUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房屋服务实现
 * @author gaowenfeng
 */
@Service
@Slf4j
public class HouseServiceImpl implements IHouseService{
    private static final String LOG_PRE = "房屋信息服务>";

    @Autowired
    private HouseRepository houseRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private HouseSubscribeRespository subscribeRespository;

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        HouseDetail detail = new HouseDetail();
        // 1.初始化房源详情
        ServiceResult<HouseDTO> result = wrapperDetailInfo(detail,houseForm);
        if(null!=result){
            return result;
        }

        //2.初始化并保存房源信息
        House house = buildHouse(houseForm);
        house = houseRepository.save(house);

        //3.保存房源详情
        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);

        //4.初始化并保存房源图片
        List<HousePicture> pictures = generatePictures(houseForm,house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(pictures);

        //5.封装返回结果
        HouseDTO houseDTO = modelMapper.map(house,HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail,HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture,HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(this.cdnPrefix+houseDTO.getCover());

        //6.保存标签信息
        List<String> tags = houseForm.getTags();
        if(null != tags || !tags.isEmpty()){
            final Long id = house.getId();
            List<HouseTag> houseTags = new ArrayList<>();
            tags.forEach(tag -> houseTags.add(new HouseTag(id,tag)));
            houseTagRepository.save(houseTags);
            houseDTO.setTags(tags);
        }

        return new ServiceResult<>(true,null,houseDTO);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult update(HouseForm houseForm) {
        House house = this.houseRepository.findOne(houseForm.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseDetail detail = this.houseDetailRepository.findByHouseId(house.getId());
        if (detail == null) {
            return ServiceResult.notFound();
        }

        ServiceResult wrapperResult = wrapperDetailInfo(detail, houseForm);
        if (wrapperResult != null) {
            return wrapperResult;
        }

        houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, houseForm.getId());
        housePictureRepository.save(pictures);

        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }

        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(new Date());
        houseRepository.save(house);

        if(HouseStatus.PASSES.getValue()==house.getStatus()){
            searchService.index(house.getId());
        }

        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch datatableSearch) {
        List<HouseDTO> houseDTOS = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.fromString(datatableSearch.getDirection()),datatableSearch.getOrderBy());
        int page = datatableSearch.getStart() / datatableSearch.getLength();

        Pageable pageable = new PageRequest(page,datatableSearch.getLength(),sort);

        Specification<House> houseSpecification = (root,query,cb) ->{
            Predicate predicate = cb.equal(root.get("adminId"),LoginUserUtil.getLoginUserId());
            predicate = cb.and(predicate,cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if(null != datatableSearch.getCity()){
                predicate = cb.and(predicate,cb.equal(root.get("cityEnName"),datatableSearch.getCity()));
            }

            if(null != datatableSearch.getStatus()){
                predicate = cb.and(predicate,cb.equal(root.get("status"),datatableSearch.getStatus()));
            }

            if(null != datatableSearch.getCreateTimeMin()){
                predicate = cb.and(predicate,cb.greaterThanOrEqualTo(root.get("createTime"),datatableSearch.getCreateTimeMin()));
            }

            if(null != datatableSearch.getCreateTimeMax()){
                predicate = cb.and(predicate,cb.lessThanOrEqualTo(root.get("createTime"),datatableSearch.getCreateTimeMax()));
            }

            if(null != datatableSearch.getTitle()){
                predicate = cb.and(predicate,cb.like(root.get("title"),"%"+datatableSearch.getTitle()+"%"));
            }

            return predicate;
        };



        Page<House> houses =  houseRepository.findAll(houseSpecification,pageable);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house,HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix+house.getCover());
            houseDTOS.add(houseDTO);
        });
        return new ServiceMultiResult<>(houses.getTotalElements(),houseDTOS);
    }

    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        House house = houseRepository.findOne(id);
        if(null == house){
            return ServiceResult.notFound();
        }

        HouseDetail detail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);

        HouseDetailDTO detailDTO = modelMapper.map(detail,HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = pictures.stream().map(picture->modelMapper.map(picture,HousePictureDTO.class)).collect(Collectors.toList());

        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);
        List<String> tagList = tags.stream().map(tag->tag.getName()).collect(Collectors.toList());

        HouseDTO result = modelMapper.map(house,HouseDTO.class);
        result.setHouseDetail(detailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

        if(LoginUserUtil.getLoginUserId()>0){
            HouseSubscribe houseSubscribe = subscribeRespository.findByHouseIdAndUserId(house.getId(),LoginUserUtil.getLoginUserId());
            if(null!=houseSubscribe){
                result.setSubscribeStatus(houseSubscribe.getStatus());
            }else {
                result.setSubscribeStatus(HouseSubscribeStatus.NO_SUBSCRIBE.getValue());
            }
        }

        return ServiceResult.of(result);
    }


    @Override
    public ServiceResult removePhoto(Long id) {
        HousePicture picture = housePictureRepository.findOne(id);
        if (picture == null) {
            return ServiceResult.notFound();
        }

        try {
            Response response = this.qiNiuService.delete(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.delete(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult updateCover(Long coverId, Long targetId) {
        HousePicture cover = housePictureRepository.findOne(coverId);
        if (cover == null) {
            return ServiceResult.notFound();
        }

        houseRepository.updateCover(targetId, cover.getPath());
        return ServiceResult.success();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult updateStatus(Long id, int status) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }

        if (house.getStatus() == status) {
            return new ServiceResult(false, "状态没有发生变化");
        }

        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "已出租的房源不允许修改状态");
        }

        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }

        houseRepository.updateStatus(id, status);

        if(HouseStatus.PASSES.getValue()==status){
            searchService.index(house.getId());
        }else {
            searchService.remove(house.getId());
        }

        return ServiceResult.success();
    }

    /**
     * 将houseId的list 构造成 HouseDTO的list
     * @param houseIds
     * @return
     */
    private List<HouseDTO> wrapperHouseResult(List<Long> houseIds){
        List<HouseDTO> result = new ArrayList<>();

        Map<Long,HouseDTO> idToHouseMap = new HashMap<>();
        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house,HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix+house.getCover());
            idToHouseMap.put(house.getId(),houseDTO);
        });

        wrapperHouselist(houseIds,idToHouseMap);

        //矫正顺序
        for(Long houseId:houseIds){
            result.add(idToHouseMap.get(houseId));
        }
        return result;
    }

    @Override
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
        if(!StringUtils.isEmpty(rentSearch.getKeywords())){
            ServiceMultiResult<Long> serviceResult = null;
            try {
                serviceResult = searchService.query(rentSearch);
            } catch (EsException e) {
                log.error(LOG_PRE+"es查询异常",e);
                return simpleQuery(rentSearch);
            }
            if(0==serviceResult.getTotal()){
                return new ServiceMultiResult<>(0,new ArrayList<>());
            }

            return new ServiceMultiResult<>(serviceResult.getTotal(),
                    wrapperHouseResult(serviceResult.getResult()));
        }

        return simpleQuery(rentSearch);
    }

    @Override
    public ServiceMultiResult<HouseDTO> wholeMapQuery(MapSearch mapSearch) {
        ServiceMultiResult<Long> serviceResult = searchService.mapQuery(mapSearch.getCityEnName(),
                mapSearch.getOrderBy(),mapSearch.getOrderDirection(),
                mapSearch.getStart(),mapSearch.getSize());
        return handleServiceMultiResult(serviceResult);
    }

    @Override
    public ServiceMultiResult<HouseDTO> boundMapQuery(MapSearch mapSearch) {
        ServiceMultiResult<Long> serviceResult = searchService.mapQuery(mapSearch);
        return handleServiceMultiResult(serviceResult);
    }

    @Override
    @Transactional
    public ServiceResult addSubscribeOrder(Long houseId) {
        Long userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe != null) {
            return new ServiceResult(false, "已加入预约");
        }

        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return new ServiceResult(false, "查无此房");
        }

        subscribe = new HouseSubscribe();
        Date now = new Date();
        subscribe.setCreateTime(now);
        subscribe.setLastUpdateTime(now);
        subscribe.setUserId(userId);
        subscribe.setHouseId(houseId);
        subscribe.setStatus(HouseSubscribeStatus.IN_ORDER_LIST.getValue());
        subscribe.setAdminId(house.getAdminId());
        subscribeRespository.save(subscribe);
        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<Pair<HouseDTO, HouseSubscribeDTO>> querySubscribeList(
            HouseSubscribeStatus status,
            int start,
            int size) {
        Long userId = LoginUserUtil.getLoginUserId();
        Pageable pageable = new PageRequest(start / size, size, new Sort(Sort.Direction.DESC, "createTime"));

        Page<HouseSubscribe> page = subscribeRespository.findAllByUserIdAndStatus(userId, status.getValue(), pageable);

        return wrapper(page);
    }

    @Override
    @Transactional
    public ServiceResult subscribe(Long houseId, Date orderTime, String telephone, String desc) {
        Long userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe == null) {
            return new ServiceResult(false, "无预约记录");
        }

        if (subscribe.getStatus() != HouseSubscribeStatus.IN_ORDER_LIST.getValue()) {
            return new ServiceResult(false, "无法预约");
        }

        subscribe.setStatus(HouseSubscribeStatus.IN_ORDER_TIME.getValue());
        subscribe.setLastUpdateTime(new Date());
        subscribe.setTelephone(telephone);
        subscribe.setDesc(desc);
        subscribe.setOrderTime(orderTime);
        subscribeRespository.save(subscribe);
        return ServiceResult.success();
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public ServiceResult cancelSubscribe(Long houseId) {
        Long userId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(houseId, userId);
        if (subscribe == null) {
            return new ServiceResult(false, "无预约记录");
        }

        subscribeRespository.delete(subscribe.getId());
        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<Pair<HouseDTO, HouseSubscribeDTO>> findSubscribeList(int start, int size) {
        Long userId = LoginUserUtil.getLoginUserId();
        Pageable pageable = new PageRequest(start / size, size, new Sort(Sort.Direction.DESC, "orderTime"));

        Page<HouseSubscribe> page = subscribeRespository.findAllByAdminIdAndStatus(userId, HouseSubscribeStatus.IN_ORDER_TIME.getValue(), pageable);

        return wrapper(page);
    }

    @Override
    @Transactional
    public ServiceResult finishSubscribe(Long houseId) {
        Long adminId = LoginUserUtil.getLoginUserId();
        HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndAdminId(houseId, adminId);
        if (subscribe == null) {
            return new ServiceResult(false, "无预约记录");
        }

        subscribeRespository.updateStatus(subscribe.getId(), HouseSubscribeStatus.FINISH.getValue());
        houseRepository.updateWatchTimes(houseId);
        return ServiceResult.success();
    }

    /**
     * 简单查询
     * @param rentSearch
     * @return
     */
    private ServiceMultiResult<HouseDTO> simpleQuery(RentSearch rentSearch) {
        //1.拼接sql
        Sort sort = HouseSort.generateSort(rentSearch.getOrderBy(),rentSearch.getOrderDirection());
        int page = rentSearch.getStart()/rentSearch.getSize();

        Pageable pageable = new PageRequest(page,rentSearch.getSize(),sort);
        Specification<House> houseSpecification = (root, criteriaQuery, criteriaBuilder)->{
            Predicate predicate = criteriaBuilder.equal(root.get("status"), HouseStatus.PASSES.getValue());
            predicate = criteriaBuilder.and(predicate,criteriaBuilder.equal(root.get("cityEnName"),rentSearch.getCityEnName()));

            if(HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())){
                predicate = criteriaBuilder.and(predicate,criteriaBuilder.gt(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY),-1));
            }
            return predicate;
        };

        Page<House> houses = houseRepository.findAll(houseSpecification,pageable);

        //2.拼接返回值
        List<HouseDTO> houseDTOS = new ArrayList<>();

        List<Long> houseIds = new ArrayList<>();
        Map<Long,HouseDTO> idToHouseMap = Maps.newHashMap();
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house,HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix+house.getCover());
            houseDTOS.add(houseDTO);
            houseIds.add(house.getId());
            idToHouseMap.put(house.getId(),houseDTO);
        });

        wrapperHouselist(houseIds,idToHouseMap);
        return new ServiceMultiResult<>(houses.getTotalElements(),houseDTOS);
    }

    /**
     * 构造房屋list
     * @param houseIds
     * @param idToHouseMap
     */
    private void wrapperHouselist(List<Long> houseIds,Map<Long,HouseDTO> idToHouseMap){
        List<HouseDetail> details = houseDetailRepository.findAllByHouseIdIn(houseIds);
        details.forEach(houseDetail -> {
            HouseDTO houseDTO = idToHouseMap.get(houseDetail.getHouseId());
            HouseDetailDTO detailDTO = modelMapper.map(houseDetail,HouseDetailDTO.class);
            houseDTO.setHouseDetail(detailDTO);
        });

        List<HouseTag> houseTags = houseTagRepository.findAllByHouseIdIn(houseIds);
        houseTags.forEach(tag ->{
            HouseDTO houseDTO = idToHouseMap.get(tag.getHouseId());
            houseDTO.getTags().add(tag.getName());
        });
    }

    /**
     * 使用HouseForm 构造 房屋实体
     * @param houseForm
     * @return
     */
    private House buildHouse(HouseForm houseForm) {
        House house = new House();
        modelMapper.map(houseForm,house);

        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLoginUserId());
        return house;
    }

    /**
     * 构造图片实体列表
     * @param form
     * @param houseId
     * @return
     */
    private List<HousePicture> generatePictures(HouseForm form,Long houseId){
        List<HousePicture> pictures = new ArrayList<>();
        if(null == form.getPhotos() || form.getPhotos().isEmpty()){
            return pictures;
        }

        for(PhotoForm photoForm:form.getPhotos()){
            HousePicture picture = modelMapper.map(photoForm,HousePicture.class);
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            pictures.add(picture);
        }

        return pictures;
    }

    /**
     * 初始化房屋详情
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm){
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if(null == subway){
            return new ServiceResult<>(false,"Not valid subway line!");
        }

        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if(null == subwayStation || !subway.getId().equals(subwayStation.getSubwayId())){
            return new ServiceResult<>(false,"Not valid subway station!");
        }


        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());

        return null;
    }


    /**
     * 将ServiceMultiResult<Long> 转换为 ServiceMultiResult<HouseDTO>
     * @param serviceResult
     * @return
     */
    private ServiceMultiResult<HouseDTO> handleServiceMultiResult(ServiceMultiResult<Long> serviceResult){

        if(0==serviceResult.getTotal()){
            return new ServiceMultiResult<>(0,new ArrayList<>());
        }
        List<HouseDTO> houses = wrapperHouseResult(serviceResult.getResult());
        return new ServiceMultiResult<>(serviceResult.getTotal(),houses);
    }

    private ServiceMultiResult<Pair<HouseDTO, HouseSubscribeDTO>> wrapper(Page<HouseSubscribe> page) {
        List<Pair<HouseDTO, HouseSubscribeDTO>> result = new ArrayList<>();

        if (page.getSize() < 1) {
            return new ServiceMultiResult<>(page.getTotalElements(), result);
        }

        List<HouseSubscribeDTO> subscribeDTOS = new ArrayList<>();
        List<Long> houseIds = new ArrayList<>();
        page.forEach(houseSubscribe -> {
            subscribeDTOS.add(modelMapper.map(houseSubscribe, HouseSubscribeDTO.class));
            houseIds.add(houseSubscribe.getHouseId());
        });

        Map<Long, HouseDTO> idToHouseMap = new HashMap<>(subscribeDTOS.size());
        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            idToHouseMap.put(house.getId(), modelMapper.map(house, HouseDTO.class));
        });

        for (HouseSubscribeDTO subscribeDTO : subscribeDTOS) {
            Pair<HouseDTO, HouseSubscribeDTO> pair = Pair.of(idToHouseMap.get(subscribeDTO.getHouseId()), subscribeDTO);
            result.add(pair);
        }

        return new ServiceMultiResult<>(page.getTotalElements(), result);
    }


}
