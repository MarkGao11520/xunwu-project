package com.gwf.xunwu.repository;

import java.util.List;

import com.gwf.xunwu.entity.HousePicture;
import org.springframework.data.repository.CrudRepository;


/**
 * Created by 瓦力.
 */
public interface HousePictureRepository extends CrudRepository<HousePicture, Long> {
    List<HousePicture> findAllByHouseId(Long id);
}
