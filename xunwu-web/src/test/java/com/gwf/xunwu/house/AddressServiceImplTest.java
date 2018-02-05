package com.gwf.xunwu.house;

import com.gwf.xunwu.ApplicationTest;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.search.BaiduMapLocation;
import com.gwf.xunwu.facade.service.house.IAddressService;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

public class AddressServiceImplTest extends ApplicationTest{
    @Autowired
    private IAddressService iAddressService;

    @org.junit.Test
    public void getBaiduMapLocation() {
        String city = "北京";
        String address = "北京市通州区珠江四季悦城13号楼1单元";
        ServiceResult<BaiduMapLocation> result = iAddressService.getBaiduMapLocation(city,address);
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getResult().getLongitude()>0);
        Assert.assertTrue(result.getResult().getLatitude()>0);
    }
}
