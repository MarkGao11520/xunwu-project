package com.gwf.xunwu.service.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.gwf.xunwu.entity.House;
import com.gwf.xunwu.entity.HouseDetail;
import com.gwf.xunwu.entity.HouseTag;
import com.gwf.xunwu.entity.SupportAddress;
import com.gwf.xunwu.facade.base.ApiResponse;
import com.gwf.xunwu.facade.base.HouseSort;
import com.gwf.xunwu.facade.base.RentValueBlock;
import com.gwf.xunwu.facade.dto.BaiduLbsDTO;
import com.gwf.xunwu.facade.exception.EsException;
import com.gwf.xunwu.facade.form.MapSearch;
import com.gwf.xunwu.facade.form.RentSearch;
import com.gwf.xunwu.facade.result.ServiceMultiResult;
import com.gwf.xunwu.facade.result.ServiceResult;
import com.gwf.xunwu.facade.search.*;
import com.gwf.xunwu.facade.service.house.IAddressService;
import com.gwf.xunwu.facade.service.search.ISearchService;
import com.gwf.xunwu.repository.HouseDetailRepository;
import com.gwf.xunwu.repository.HouseRepository;
import com.gwf.xunwu.repository.HouseTagRepository;
import com.gwf.xunwu.repository.SupportAddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ISearchService 实现
 * @author gaowenfeng
 */
@Service
@Slf4j
public class SearchServiceImpl implements ISearchService{

    private static final String LOG_PRE = "es查询服务->";

    private static final String INDEX_NAME = "xunwu";

    private static final String INDEX_TYPE = "house";

    private static final String INDEX_TOPIC = "xunwu_topic";

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private IAddressService addressService;

    /**
     * 异步监听消息
     * @param content
     */
    @KafkaListener(topics = INDEX_TOPIC)
    private void handleMessage(String content){
        try {
            HouseIndexMessage message = objectMapper.readValue(content, HouseIndexMessage.class);

            switch (message.getOperation()){
                case HouseIndexMessage.INDEX:
                    this.createOrUpdateIndex(message);
                    break;
                case HouseIndexMessage.REMOVE:
                    this.removeIndex(message);
                    break;
                default:
                    log.warn(LOG_PRE+"Not support message content {}",content);
                    break;
            }
        } catch (IOException e) {
            log.error(LOG_PRE+" Cannot parse json for "+content,e);
        }
    }

    @Override
    public void index(Long houseId) {
        this.index(houseId,0);
    }

    private void index(long houseId,int retry){
        sendMessage(houseId, retry, HouseIndexMessage.INDEX);
    }

    @Override
    public void remove(Long houseId) {
        this.remove(houseId,0);
    }

    @Override
    public ServiceMultiResult<Long> query(RentSearch search) throws EsException {
        /**
         * 1.拼接查询条件
         *  1.1 城市名称
         *  1.2 若地区查询条件不为空，则添加地区过滤
         *  1.3 位置查询
         *  1.4 价格查询
         *  1.5 方向及地铁站
         *  1.6 关键词
         * 2.封装返回结果
         */
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME,search.getCityEnName())
            );
            if(null!=search.getRegionEnName()&&!RentSearch.regionAll.equals(search.getRegionEnName())){
                boolQuery.filter(
                  QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME,search.getRegionEnName())
                );
            }

            RentValueBlock area = RentValueBlock.matchArea(search.getAreaBlock());
            if(!RentValueBlock.ALL.equals(area)){
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
                if(area.getMax()>0){
                    rangeQueryBuilder.lte(area.getMax());
                }
                if(area.getMin()<0){
                    rangeQueryBuilder.gte(area.getMin());
                }
                boolQuery.filter(rangeQueryBuilder);
            }

            RentValueBlock price = RentValueBlock.matchPrice(search.getPriceBlock());
            if(!RentValueBlock.ALL.equals(price)){
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);
                if(price.getMax()>0){
                    rangeQueryBuilder.lte(price.getMax());
                }
                if(price.getMin()>0){
                    rangeQueryBuilder.gte(price.getMin());
                }
                boolQuery.filter(rangeQueryBuilder);
            }

            if(search.getDirection()>0){
                boolQuery.filter(
                        QueryBuilders.termQuery(HouseIndexKey.DIRECTION,search.getDirection())
                );
            }

            if(search.getRentWay()>-1){
                boolQuery.filter(
                        QueryBuilders.termQuery(HouseIndexKey.RENT_WAY,search.getRentWay())
                );
            }

            // TODO 关键词搜索的时候，加上其他条件，关键词几乎不起作用了，需要修改
            boolQuery.should(QueryBuilders.matchQuery(HouseIndexKey.TITLE,search.getKeywords()).boost(2.0f))
                    ;

            boolQuery.should(
                    QueryBuilders.multiMatchQuery(search.getKeywords(),
                        HouseIndexKey.TRAFFIC,
                        HouseIndexKey.DISTRICT,
                        HouseIndexKey.ROUND_SERVICE,
                        HouseIndexKey.SUBWAY_LINE_NAME,
                        HouseIndexKey.SUBWAY_STATION_NAME
                    ));

            SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                    .setTypes(INDEX_TYPE)
                    .setQuery(boolQuery)
                    .addSort(
                            HouseSort.getSortKey(search.getOrderBy()),
                            SortOrder.fromString(search.getOrderDirection())
                    )
                    .setFrom(search.getStart())
                    .setSize(search.getSize())
                    .setFetchSource(HouseIndexKey.HOUSE_ID,null);

            log.debug(LOG_PRE+"requestBuilder=[{}]",requestBuilder.toString());

            List<Long> houseIds = new ArrayList<>();
            SearchResponse searchResponse = requestBuilder.get();

            if(RestStatus.OK != searchResponse.status()){
                log.warn(LOG_PRE+"Search status is no ok for {}",requestBuilder);
                return new ServiceMultiResult<>(0,houseIds);
            }

            for(SearchHit hit:searchResponse.getHits()){
                houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID))));
            }

            return new ServiceMultiResult<>(searchResponse.getHits().totalHits,houseIds);
        } catch (Exception e) {
            throw new EsException(ApiResponse.Status.INTERNAL_SERVER_ERROR.getCode(),"es查询异常");
        }
    }

    @Override
    public ServiceResult<List<String>> suggest(String prefix) {
        int maxSuggest = 5;
        CompletionSuggestionBuilder suggestion = SuggestBuilders.completionSuggestion("suggest")
                .prefix(prefix).size(5);

        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("autocomplete",suggestion);


        SearchRequestBuilder requestBuilder =  this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .suggest(suggestBuilder);

        log.debug(LOG_PRE+requestBuilder.toString());

        SearchResponse response = requestBuilder.get();
        Suggest suggest = response.getSuggest();
        Suggest.Suggestion result = suggest.getSuggestion("autocomplete");

        Set<String> suggestSet = new HashSet<>();
        for(Object term : result.getEntries()){
            if(term instanceof CompletionSuggestion.Entry){
                CompletionSuggestion.Entry item = (CompletionSuggestion.Entry) term;

                if(item.getOptions().isEmpty()){
                    continue;
                }

                for (CompletionSuggestion.Entry.Option option:item.getOptions()){
                    String tip = option.getText().string();
                    suggestSet.add(tip);
                }
            }

            if(suggestSet.size()>maxSuggest){
                break;
            }
        }
        List<String> suggestList = Lists.newArrayList(suggestSet.toArray(new String[]{}));
        return ServiceResult.of(suggestList);
    }

    @Override
    public ServiceResult<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME,cityEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME,regionEnName))
                .filter(QueryBuilders.termQuery(HouseIndexKey.DISTRICT,district));

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addAggregation(
                        AggregationBuilders.terms(HouseIndexKey.AGG_DISTRICT)
                        .field(HouseIndexKey.DISTRICT)
                ).setSize(0);

        log.debug(LOG_PRE+"es聚合房源信息：requestBuilder=[{}]",requestBuilder.toString());

        SearchResponse response = requestBuilder.get();
        if(RestStatus.OK == response.status()){
            Terms terms = response.getAggregations().get(HouseIndexKey.AGG_DISTRICT);
            if(!StringUtils.isEmpty(terms.getBuckets())){
                return ServiceResult.of(terms.getBucketByKey(district).getDocCount());
            }
        }else {
            log.warn(LOG_PRE+"Failed to Aggregate for "+HouseIndexKey.AGG_DISTRICT);
        }
        return ServiceResult.of(0L);
    }

    @Override
    public ServiceMultiResult<HouseBucketBO> mapAggregate(String cityEnName) {

        /**
         * 1.根据城市查询
         * 2.聚合地区信息
         * 3.构造查询
         * 4.判断返回结果
         */
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME,cityEnName));
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms(HouseIndexKey.AGG_REGION)
                .field(HouseIndexKey.REGION_EN_NAME);

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQueryBuilder)
                .addAggregation(aggregationBuilder);
        log.debug(LOG_PRE+"聚合城市数据requestBuilder=[{}]",requestBuilder);

        SearchResponse response = requestBuilder.get();

        List<HouseBucketBO> result = new ArrayList<>();
        if(RestStatus.OK!=response.status()){
            log.warn(LOG_PRE+"Failed to Aggregate for [{}]",HouseIndexKey.AGG_REGION);
            return new ServiceMultiResult<>(0,result);
        }

        Terms terms = response.getAggregations().get(HouseIndexKey.AGG_REGION);
        for (Terms.Bucket bucket : terms.getBuckets()) {
            result.add(new HouseBucketBO(bucket.getKey().toString(),bucket.getDocCount()));
        }

        return new ServiceMultiResult<>(response.getHits().getTotalHits(),result);


    }

    @Override
    public ServiceMultiResult<Long> mapQuery(String cityEnName,
                                             String orderBy, String orderDirection,
                                             int start, int size) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME,cityEnName));

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQueryBuilder)
                .addSort(HouseSort.getSortKey(orderBy),SortOrder.fromString(orderDirection))
                .setFrom(start)
                .setSize(size);

        SearchResponse response = requestBuilder.get();
        if(RestStatus.OK!=response.status()){
            log.warn(LOG_PRE+"Search status is not ok for "+requestBuilder);
            return new ServiceMultiResult<>(0,new ArrayList<>());
        }
        List<Long> houseIds = new ArrayList<>();
        for (SearchHit hitFields : response.getHits()) {
            houseIds.add(Long.parseLong(String.valueOf(hitFields.getSource().get(HouseIndexKey.HOUSE_ID))));
        }
        return new ServiceMultiResult<>(response.getHits().getTotalHits(),houseIds);
    }

    @Override
    public ServiceMultiResult<Long> mapQuery(MapSearch mapSearch) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME,mapSearch.getCityEnName()));
        boolQueryBuilder.filter(QueryBuilders.geoBoundingBoxQuery(HouseIndexKey.LOCATION).setCorners(
                new GeoPoint(mapSearch.getLeftLatitude(),mapSearch.getLeftLongitude()),
                new GeoPoint(mapSearch.getRightLatitude(),mapSearch.getRightLongitude())
        ));

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQueryBuilder)
                .addSort(HouseSort.getSortKey(mapSearch.getOrderBy()),SortOrder.fromString(mapSearch.getOrderDirection()))
                .setFrom(mapSearch.getStart())
                .setSize(mapSearch.getSize());

        SearchResponse response = requestBuilder.get();
        if(RestStatus.OK!=response.status()){
            log.warn(LOG_PRE+"Search status is not ok for "+requestBuilder);
            return new ServiceMultiResult<>(0,new ArrayList<>());
        }
        List<Long> houseIds = new ArrayList<>();
        for (SearchHit hitFields : response.getHits()) {
            houseIds.add(Long.parseLong(String.valueOf(hitFields.getSource().get(HouseIndexKey.HOUSE_ID))));
        }
        return new ServiceMultiResult<>(response.getHits().getTotalHits(),houseIds);
    }


    private boolean updateSuggest(HouseIndexTemplate indexTemplate){
        AnalyzeRequestBuilder requestBuilder = new AnalyzeRequestBuilder(
                this.esClient,
                AnalyzeAction.INSTANCE,INDEX_NAME,
                indexTemplate.getTitle(),
                indexTemplate.getLayoutDesc(),
                indexTemplate.getRoundService(),
                indexTemplate.getDescription(),
                indexTemplate.getSubwayLineName(),
                indexTemplate.getSubwayStationName());

        requestBuilder.setAnalyzer("ik_smart");

        AnalyzeResponse response = requestBuilder.get();
        List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens();
        if(null == tokens){
            log.warn(LOG_PRE+"Can not analyze token for house: "+indexTemplate.getHouseId());
            return false;
        }

        List<HouseSuggest> suggests = new ArrayList<>();
        for(AnalyzeResponse.AnalyzeToken token:tokens){
            // 排序数字类型 & 小宇两个字符的分词结果
            if("<NUM>".equals(token.getType()) || token.getTerm().length()<2){
                continue;
            }

            HouseSuggest suggest = new HouseSuggest();
            suggest.setInput(token.getTerm());
            suggests.add(suggest);
            suggests.add(suggest);
        }

        // 定制化小区
        HouseSuggest suggest = new HouseSuggest();
        suggest.setInput(indexTemplate.getDistrict());
        suggests.add(suggest);

        indexTemplate.setSuggest(suggests);
        return true;
    }

    private void remove(Long houseId, int retry) {
        sendMessage(houseId, retry, HouseIndexMessage.REMOVE);
    }

    private void sendMessage(Long houseId, int retry, String operation) {
        if (HouseIndexMessage.MAX_RETRY < retry) {
            StringBuilder error = new StringBuilder();
            error.append("Retry ")
                    .append(operation)
                    .append("times over 3 for house:")
                    .append(houseId)
                    .append("Please check it !");
            log.error(LOG_PRE+error.toString());
            return;
        }
        HouseIndexMessage message = new HouseIndexMessage(houseId, operation, retry);
        try {
            this.kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error(LOG_PRE+"Json encode error for {}",message);
        }
    }

    /**
     * 创建索引
     * @param indexTemplate
     * @return
     */
    private boolean create(HouseIndexTemplate indexTemplate){
        return createOrUpdate(indexTemplate,Thread.currentThread().getStackTrace()[1].getMethodName(),()->
            this.esClient
                    .prepareIndex(INDEX_NAME,INDEX_TYPE)
                    .setSource(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON)
                    .get()
        );
    }

    /**
     * 更新索引
     * @param esId
     * @param indexTemplate
     * @return
     */
    private boolean update(String esId,HouseIndexTemplate indexTemplate){
        return createOrUpdate(indexTemplate, Thread.currentThread().getStackTrace()[1].getMethodName(),()->
                this.esClient
                        .prepareUpdate(INDEX_NAME,INDEX_TYPE,esId)
                        .setDoc(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON)
                        .get()
        );
    }

    private interface Operate{
        /**
         * 操作并返回
         * @return
         * @throws JsonProcessingException
         */
        DocWriteResponse doAndReturn() throws JsonProcessingException;
    }

    private boolean createOrUpdate(HouseIndexTemplate indexTemplate,String type,Operate operate) {
        if(!updateSuggest(indexTemplate)){
            return false;
        }
        try {
            DocWriteResponse response = operate.doAndReturn();
            log.debug(LOG_PRE+"{} index with house: {}",type,indexTemplate.getHouseId());
            return (response.status() == RestStatus.OK) || (response.status()==RestStatus.CREATED) ;
        } catch (JsonProcessingException e) {
            log.error(LOG_PRE+"Error to index house {}",indexTemplate.getHouseId(),e);
            return false;
        }
    }

    /**
     * 删除并创建新索引
     * @param totalHit
     * @param indexTemplate
     * @return
     */
    private boolean deleteAndCreate(long totalHit, HouseIndexTemplate indexTemplate){
        long deleted = getDeleted(indexTemplate.getHouseId());
        if(deleted != totalHit) {
            log.warn(LOG_PRE+"Need delete {},but {} was deleted!", totalHit, deleted);
            return false;
        }else {
            return create(indexTemplate);
        }
    }

    /**
     * 删除索引并返回删除条数
     * @param houseId
     * @return
     */
    private long getDeleted(Long houseId) {
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE
                .newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))
                .source(INDEX_NAME);

        log.debug(LOG_PRE+"delete by query for house: {}",builder);

        BulkByScrollResponse response = builder.get();
        return response.getDeleted();
    }

    /**
     * 删除索引
     * @param message
     */
    private void removeIndex(HouseIndexMessage message) {
        Long houseId = message.getHouseId();
        Long deleted =  getDeleted(houseId);

        ServiceResult serviceResult = addressService.removeLBS(houseId);
        if(! serviceResult.isSuccess() || 0 >= deleted){
            // 重新加入消息队列
            this.remove(houseId,message.getRetry()+1);
        }
    }

    /**
     * 创建或者更新索引
     * @param message
     */
    private void createOrUpdateIndex(HouseIndexMessage message){
        /**
         * 1.查找房屋信息
         * 2.若房屋信息未空则重试发送消息
         * 3.创建es房屋模板
         * 4.查找房屋详情信息并放置进es模板
         * 5.若详情信息未空则处理异常情况
         * 6.查找房屋标签信息并放置进es模板
         * 7.es查询
         * 8.若无es信息则创建
         * 9.若查询结果集为1，说明已经存在，则更改
         * 10.若查询结果集多于1条，说明之前插入有异常情况，则删除重写创建
         */
        // 1.查找房屋信息
        Long houseId = message.getHouseId();
        House house = houseRepository.findOne(houseId);
        // 2.若房屋信息未空则重试发送消息
        if(null == house){
            log.error(LOG_PRE+"Index house {} dose not exist!",houseId);
            this.index(houseId,message.getRetry()+1);
            return;
        }

        // 3.创建es房屋模板
        HouseIndexTemplate indexTemplate = new HouseIndexTemplate();
        modelMapper.map(house,indexTemplate);

        // 4.查找房屋详情信息并放置进es模板
        HouseDetail detail = houseDetailRepository.findByHouseId(houseId);
        if(null == detail){
            // 5.若详情信息未空则处理异常情况
            //TODO 异常情况
        }
        modelMapper.map(detail,indexTemplate);


        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(house.getCityEnName(),SupportAddress.Level.CITY.getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndLevel(house.getRegionEnName(),SupportAddress.Level.REGION.getValue());
        String address = city.getCnName()+region.getCnName()+house.getStreet()+house.getDistrict()+detail.getDetailAddress();


        ServiceResult<BaiduMapLocation> location = addressService.getBaiduMapLocation(city.getCnName(),address);
        if(!location.isSuccess()){
            this.index(message.getHouseId(),message.getRetry()+1);
            return;
        }
        indexTemplate.setLocation(location.getResult());


        // 6.查找房屋标签信息并放置进es模板
        List<HouseTag> tags = houseTagRepository.findAllByHouseId(houseId);
        if(!CollectionUtils.isEmpty(tags)){
            List<String> tagStrings = tags.stream().map(tag -> tag.getName()).collect(Collectors.toList());
            indexTemplate.setTags(tagStrings);
        }

        // 7.es查询
        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE)
                .setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID,houseId));

        log.debug(LOG_PRE+"requestBuilder=[{}]",requestBuilder.toString());

        SearchResponse searchResponse = requestBuilder.get();
        long totalHit = searchResponse.getHits().getTotalHits();
        boolean success;
        if(0==totalHit){
            // 8.若无es信息则创建
            success = create(indexTemplate);
        }else if(1 == totalHit){
            // 9.若查询结果集为1，说明已经存在，则更改
            String esId = searchResponse.getHits().getAt(0).getId();
            success = update(esId,indexTemplate);
        }else {
            // 10.若查询结果集多于1条，说明之前插入有异常情况，则删除重写创建
            success = deleteAndCreate(totalHit,indexTemplate);
        }

        BaiduLbsDTO baiduLbsDTO = BaiduLbsDTO.builder().baiduMapLocation(location.getResult())
                .title(house.getStreet()+house.getDistrict())
                .address(city.getCnName()+region.getCnName()+house.getStreet()+house.getDistrict())
                .houseId(houseId)
                .price((long) house.getPrice())
                .area((long) house.getArea()).build();

        // 创建百度地图API服务
        ServiceResult serviceResult = addressService.lbsUpload(baiduLbsDTO);
        if(!success || !serviceResult.isSuccess()){
            this.index(message.getHouseId(),message.getRetry()+1);
        }else {
            log.debug(LOG_PRE+"Index success with house {}",houseId);
        }
    }


}
