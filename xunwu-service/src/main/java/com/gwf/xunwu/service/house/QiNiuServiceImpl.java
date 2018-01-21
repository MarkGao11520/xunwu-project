package com.gwf.xunwu.service.house;

import com.gwf.xunwu.facade.service.house.IQiNiuService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;

@Service
public class QiNiuServiceImpl implements IQiNiuService,InitializingBean{

    @Autowired
    private UploadManager uploadManager;

    @Autowired
    private BucketManager bucketManager;

    @Autowired
    private Auth auth;

    @Value("${qiniu.Bucket}")
    private String bucket;

    private StringMap putPolicy;

    @Override
    public Response uploadFile(File file) throws QiniuException {
        return qiniuUpload(() -> uploadManager.put(file,null,getUploadToken()));
    }

    @Override
    public Response uploadFile(InputStream inputStream) throws QiniuException {
        return qiniuUpload(() -> uploadManager.put(inputStream,null,getUploadToken(),null,null));
    }

    @Override
    public Response delete(String key) throws QiniuException {
        return qiniuUpload(()->bucketManager.delete(this.bucket,key));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.putPolicy = new StringMap();
        putPolicy.put("returnBody", "{\"key\":\"$(key)\"," +
                "\"hash\":\"$(etag)\"," +
                "\"bucket\":\"$(bucket)\"," +
                "\"width\":$(imageInfo.width)," +
                "\"height\":$(imageInfo.height)}");
    }

    /**
     * 获取上传凭证
     * @return
     */
    private String getUploadToken(){
        return this.auth.uploadToken(bucket,null,3600,putPolicy);
    }

    private Response qiniuUpload(UploadHandler uploadHandler) throws QiniuException{
        Response response = uploadHandler.handle();
        int retry = 0;
        while (response.needRetry() && retry<3){
            response = uploadHandler.handle();
            retry ++;
        }
        return response;
    }

    interface UploadHandler{
        Response handle()throws QiniuException;
    }
}
