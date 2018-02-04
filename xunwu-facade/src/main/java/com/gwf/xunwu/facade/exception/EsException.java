package com.gwf.xunwu.facade.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * es异常
 * @author gaowenfeng
 */
@Getter
@NoArgsConstructor
public class EsException extends Exception {
    private int code;
    private String msg;



    public EsException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
