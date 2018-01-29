package com.gwf.xunwu.web.dto;

import lombok.ToString;

/**
 * 七牛配置
 * @author gaowenfeng
 */
@ToString
public final class QiNiuPutRet {
    public String key;
    public String hash;
    public String bucket;
    public int width;
    public int height;
}
