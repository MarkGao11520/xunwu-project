package com.gwf.xunwu.facade.base;

import lombok.Data;
import lombok.ToString;

/**
 * Datatables响应结构
 * Created by 瓦力.
 */
@Data
@ToString
public class ApiDataTableResponse extends ApiResponse {
    private int draw;
    private long recordsTotal;
    private long recordsFiltered;

    public ApiDataTableResponse(Status status) {
        this(status.getCode(), status.getStandardMessage(), null);
    }

    public ApiDataTableResponse(int code, String message, Object data) {
        super(code, message, data);
    }

}
