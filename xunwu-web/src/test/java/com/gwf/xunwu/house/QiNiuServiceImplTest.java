package com.gwf.xunwu.house;

import com.gwf.xunwu.ApplicationTest;
import com.gwf.xunwu.facade.service.house.IQiNiuService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;


public class QiNiuServiceImplTest extends ApplicationTest{

    @Autowired
    private IQiNiuService iQiNiuFacade;


    @Test
    public void uploadFile() {
        String fileName = "/Users/gaowenfeng/Documents/IDE/newsell/xunwu-project/xunwu-web/tmp/2018-1-20 下午8.31 拍摄的照片.jpg";
        File file = new File(fileName);

        Assert.assertTrue(file.exists());

        try {
            Response response = iQiNiuFacade.uploadFile(file);
            Assert.assertTrue(response.isOK());
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }

    @org.junit.Test
    public void uploadFile1() {
    }

    @org.junit.Test
    public void delete() {
    }
}
