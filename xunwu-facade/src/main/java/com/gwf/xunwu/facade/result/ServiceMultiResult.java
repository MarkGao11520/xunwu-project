package com.gwf.xunwu.facade.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;

@AllArgsConstructor
@Data
public class ServiceMultiResult<T> {
    private long total;
    private List<T> result;

    public int getResultSize(){
        if(CollectionUtils.isEmpty(this.result)){
            return 0;
        }
        return result.size();
    }
}
