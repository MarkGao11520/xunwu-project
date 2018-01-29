package com.gwf.xunwu.facade.service.house;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

import java.io.File;
import java.io.InputStream;

/**
 * 七牛服务接口
 * @author gaowenfeng
 */
public interface IQiNiuService {
    /**
     * 通过文件对象上传文件
     * @param file 文件
     * @return
     * @throws QiniuException 可能由于参数或网络等未知因素导致七牛异常
     */
    Response uploadFile(File file) throws QiniuException;

    /**
     * 通过文件输入流创建对象
     * @param inputStream 输入流
     * @return
     * @throws QiniuException 可能由于参数或网络等未知因素导致七牛异常
     */
    Response uploadFile(InputStream inputStream) throws QiniuException;

    /**
     * 删除文件
     * @param key 文件在七牛服务器对应的key
     * @return
     * @throws QiniuException 可能由于参数或网络等未知因素导致七牛异常
     */
    Response delete(String key) throws QiniuException;
}
