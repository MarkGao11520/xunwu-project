package com.gwf.xunwu.web.controller.house;

import com.gwf.xunwu.entity.SupportAddress;
import com.gwf.xunwu.facade.base.ApiResponse;
import com.gwf.xunwu.facade.base.RentValueBlock;
import com.gwf.xunwu.facade.bo.*;
import com.gwf.xunwu.facade.form.RentSearch;
import com.gwf.xunwu.facade.search.HouseBucketDTO;
import com.gwf.xunwu.facade.service.house.IAddressService;
import com.gwf.xunwu.facade.service.house.IHouseService;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.service.search.ISearchService;
import com.gwf.xunwu.facade.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author gaowenfeng
 */
@RestController
public class HouseController {

    @Autowired
    private IAddressService addressService;

    @Autowired
    private IHouseService houseService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ISearchService searchService;

    /**
     * 自动补全接口
     */
    @GetMapping("rent/house/autocomplete")
    @ResponseBody
    public ApiResponse autoComplete(@RequestParam(value = "prefix") String prefix){
        if(prefix.isEmpty()){
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        ServiceResult<List<String>> result = this.searchService.suggest(prefix);
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取支持城市列表
     * @return
     */
    @GetMapping("address/support/cities")
    public ApiResponse getSupportCities() {
        ServiceMultiResult<SupportAddressBO> result = addressService.findAllCities();
        if (result.getResultSize() == 0) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取对应城市支持区域列表
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/regions")
    public ApiResponse getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        ServiceMultiResult<SupportAddressBO> addressResult = addressService.findAllRegionsByCityName(cityEnName);
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(addressResult.getResult());
    }

    /**
     * 获取具体城市所支持的地铁线路
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/subway/line")
    public ApiResponse getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayBO> subways = addressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     * @param subwayId
     * @return
     */
    @GetMapping("address/support/subway/station")
    public ApiResponse getSupportSubwayStation(@RequestParam(name = "subway_id") Long subwayId) {
        List<SubwayStationBO> stationDTOS = addressService.findAllStationBySubway(subwayId);
        if (stationDTOS.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(stationDTOS);
    }

    @GetMapping("rent/house")
    public ModelAndView rentHousePage(@ModelAttribute RentSearch rentSearch,
                                      ModelAndView mv, HttpSession session,
                                      RedirectAttributes redirectAttributes){
        //1.判断cityEnName参数的合法性
        if(null == rentSearch.getCityEnName()){
            String cityEnNameInSession = (String) session.getAttribute("cityEnName");
            if(null == cityEnNameInSession){
                redirectAttributes.addAttribute("msg","must_chose_city");
                mv.setViewName("redirect:/index");
                return mv;
            }else {
                rentSearch.setCityEnName(cityEnNameInSession);
            }
        }else {
            session.setAttribute("cityEnName",rentSearch.getCityEnName());
        }

        ServiceResult<SupportAddressBO> city =  addressService.findCity(rentSearch.getCityEnName());
        if(!city.isSuccess()){
            redirectAttributes.addAttribute("msg","must_chose_city");
            mv.setViewName("redirect:/index");
            return mv;
        }

        mv.addObject("currentCity",city.getResult());

        //2.查找区域信息
        ServiceMultiResult<SupportAddressBO> addressResult = addressService.findAllRegionsByCityName(rentSearch.getCityEnName());

        if(null==addressResult.getResult() || 1>addressResult.getTotal()){
            redirectAttributes.addAttribute("msg","must_chose_city");
            mv.setViewName("redirect:/index");
            return mv;
        }

        if(null == rentSearch.getRegionEnName()){
            rentSearch.setRegionEnName(RentSearch.regionAll);
        }

        //3.查询以及构造返回结果集
        ServiceMultiResult<HouseBO> result = houseService.query(rentSearch);
        mv.addObject("total",result.getTotal());
        mv.addObject("houses",result.getResult());

        mv.addObject("searchBody",rentSearch);
        mv.addObject("regions",addressResult.getResult());

        mv.addObject("priceBlocks", RentValueBlock.PRICE_BLOCK);
        mv.addObject("areaBlocks",RentValueBlock.AREA_BLOCK);

        mv.addObject("currentPriceBlock",RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
        mv.addObject("currentAreaBlock",RentValueBlock.matchArea(rentSearch.getAreaBlock()));

        mv.setViewName("rent-list");
        return mv;
    }

    @GetMapping("rent/house/show/{id}")
    public ModelAndView show(@PathVariable(value = "id") Long houseId,
                       ModelAndView mv) {
        if (houseId <= 0) {
            mv.setViewName("404");
            return mv;
        }

        ServiceResult<HouseBO> serviceResult = houseService.findCompleteOne(houseId);
        if (!serviceResult.isSuccess()) {
            mv.setViewName("404");
            return mv;
        }

        HouseBO houseDTO = serviceResult.getResult();
        Map<SupportAddress.Level, SupportAddressBO>
                addressMap = addressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());

        SupportAddressBO city = addressMap.get(SupportAddress.Level.CITY);
        SupportAddressBO region = addressMap.get(SupportAddress.Level.REGION);

        mv.addObject("city", city);
        mv.addObject("region", region);

        ServiceResult<UserBO> userDTOServiceResult = userService.findById(houseDTO.getAdminId());
        mv.addObject("agent", userDTOServiceResult.getResult());
        mv.addObject("house", houseDTO);

        ServiceResult<Long> aggregateDistrictHouse = searchService.aggregateDistrictHouse(city.getEnName(),region.getEnName(),houseDTO.getDistrict());

        mv.addObject("houseCountInDistrict",aggregateDistrictHouse.getResult());
        mv.setViewName("house-detail");
        return mv;
    }

    @GetMapping("rent/house/map")
    public ModelAndView rentHouseMap(@RequestParam("cityEnName")String cityEnName,
                                     ModelAndView mv,
                                     RedirectAttributes redirectAttributes,
                                     HttpSession session){
        ServiceResult<SupportAddressBO> city = addressService.findCity(cityEnName);
        if(!city.isSuccess()){
            redirectAttributes.addAttribute("msg","must_chose_city");
            mv.setViewName("index");
            return mv;
        }else {
            session.setAttribute("cityName",cityEnName);
            mv.addObject("city",city.getResult());
        }

        ServiceMultiResult<SupportAddressBO> regions = addressService.findAllRegionsByCityName(cityEnName);
        ServiceMultiResult<HouseBucketDTO> buckets = searchService.mapAggregate(cityEnName);

        mv.addObject("aggData",buckets.getResult());
        mv.addObject("total",buckets.getTotal());
        mv.addObject("regions",regions);
        mv.setViewName("rent-map");
        return mv;
    }
}
