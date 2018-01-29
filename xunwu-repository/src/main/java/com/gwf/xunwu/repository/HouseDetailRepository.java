package com.gwf.xunwu.repository;

import java.util.List;

import com.gwf.xunwu.entity.HouseDetail;
import org.springframework.data.repository.CrudRepository;


/**
 * 房屋详情数据库持久层接口
 * @author gaowenfeng
 */
public interface HouseDetailRepository extends CrudRepository<HouseDetail, Long>{
    /**
     * 根据id查找房屋
     * @param houseId
     * @return
     */
    HouseDetail findByHouseId(Long houseId);

    /**
     * 根据房屋id列表查找房屋详情
     * @param houseIds
     * @return
     */
    List<HouseDetail> findAllByHouseIdIn(List<Long> houseIds);
}
