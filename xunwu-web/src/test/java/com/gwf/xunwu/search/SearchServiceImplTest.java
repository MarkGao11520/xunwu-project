package com.gwf.xunwu.search;

import com.gwf.xunwu.ApplicationTest;
import com.gwf.xunwu.facade.search.HouseIndexTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;

public class SearchServiceImplTest extends ApplicationTest{

    @Autowired
    private SearchServiceImpl searchService;

    private long houseId = 15l;

    @org.junit.Test
    public void index() {
        searchService.index(houseId);
    }

    @org.junit.Test
    public void remove() {
        searchService.remove(houseId);
    }

    @org.junit.Test
    public void create() {
        ArrayList<String> list = new ArrayList<>();
        list.add("aaa");
        HouseIndexTemplate template = HouseIndexTemplate.builder()
                .area(1)
                .cityEnName("北京")
                .createTime(new Date())
                .description("aaaa")
                .distanceToSubway(1)
                .direction(1)
                .district("aaa")
                .houseId(1l)
                .lastUpdateTime(new Date())
                .layoutDesc("aaa")
                .price(111)
                .regionEnName("通州区")
                .rentWay(1)
                .roundService("a")
                .title("啊啊啊啊")
                .traffic("lulu")
                .street("13街")
                .subwayLineName("13号线")
                .subwayStationName("亦庄")
                .tags(list).build();
        searchService.index(1l);
    }
}
