package com.gwf.xunwu.facade.form;

import lombok.Data;
import lombok.ToString;

/**
 * 照片表单
 * @author gaowenfeng
 */
@Data
@ToString
public class PhotoForm {
    private String path;

    private int width;

    private int height;

}
