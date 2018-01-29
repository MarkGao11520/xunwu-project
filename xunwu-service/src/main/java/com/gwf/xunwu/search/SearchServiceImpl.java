package com.gwf.xunwu.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwf.xunwu.entity.House;
import com.gwf.xunwu.entity.HouseDetail;
import com.gwf.xunwu.entity.HouseTag;
import com.gwf.xunwu.facade.search.HouseIndexMessage;
import com.gwf.xunwu.facade.service.search.ISearchService;
import com.gwf.xunwu.facade.search.HouseIndexKey;
import com.gwf.xunwu.facade.search.HouseIndexTemplate;
import com.gwf.xunwu.repository.HouseDetailRepository;
import com.gwf.xunwu.repository.HouseRepository;
import com.gwf.xunwu.repository.HouseTagRepository;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ISearchService 实现
 * @author gaowenfeng
 */
@Service
@Slf4j
public class SearchServiceImpl implements ISearchService{

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
                    log.warn("Not support message content "+content);
                    break;
            }
        } catch (IOException e) {
            log.error("Cannot parse json for "+content,e);
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
            log.error(error.toString());
            return;
        }
        HouseIndexMessage message = new HouseIndexMessage(houseId, operation, retry);
        try {
            this.kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Json encode error for " + message);
        }
    }

    /**
     * 创建索引
     * @param indexTemplate
     * @return
     */
    private boolean create(HouseIndexTemplate indexTemplate){
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(indexTemplate);
            IndexResponse response = this.esClient.prepareIndex(INDEX_NAME,INDEX_TYPE)
                    .setSource(bytes, XContentType.JSON).get();

            log.debug("Create index with house: "+indexTemplate.getHouseId());
            if (response.status() == RestStatus.CREATED) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            log.error("Error to index house "+indexTemplate.getHouseId(),e);
            return false;
        }
    }

    /**
     * 更新索引
     * @param esId
     * @param indexTemplate
     * @return
     */
    private boolean update(String esId,HouseIndexTemplate indexTemplate){
        try {
            UpdateResponse response = this.esClient.prepareUpdate(INDEX_NAME,INDEX_TYPE,esId)
                    .setDoc(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get();

            log.debug("Update index with house: "+indexTemplate.getHouseId());
            if (response.status() == RestStatus.OK) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            log.error("Error to index house "+indexTemplate.getHouseId(),e);
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
            log.warn("Need delete {},but {} was deleted!", totalHit, deleted);
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

        log.debug("delete by query for house: " + builder);

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

        if(0 >= deleted){
            this.remove(houseId,message.getRetry()+1);
        }
    }

    /**
     * 创建或者更新索引
     * @param message
     */
    private void createOrUpdateIndex(HouseIndexMessage message){
        Long houseId = message.getHouseId();
        House house = houseRepository.findOne(houseId);
        if(null == house){
            log.error("Index house {} dose not exist!",houseId);
            this.index(houseId,message.getRetry()+1);
            return;
        }

        HouseIndexTemplate indexTemplate = new HouseIndexTemplate();
        modelMapper.map(house,indexTemplate);

        HouseDetail detail = houseDetailRepository.findByHouseId(houseId);
        if(null == detail){
            //TODO 异常情况
        }
        modelMapper.map(detail,indexTemplate);

        List<HouseTag> tags = houseTagRepository.findAllByHouseId(houseId);
        if(!CollectionUtils.isEmpty(tags)){
            List<String> tagStrings = tags.stream().map(tag -> tag.getName()).collect(Collectors.toList());
            indexTemplate.setTags(tagStrings);
        }

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE)
                .setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID,houseId));

        log.debug(requestBuilder.toString());

        SearchResponse searchResponse = requestBuilder.get();
        long totalHit = searchResponse.getHits().getTotalHits();
        boolean success;
        if(0==totalHit){
            success = create(indexTemplate);
        }else if(1 == totalHit){
            String esId = searchResponse.getHits().getAt(0).getId();
            success = update(esId,indexTemplate);
        }else {
            success = deleteAndCreate(totalHit,indexTemplate);
        }
        if(success){
            log.debug("Index success with house" +houseId);
        }
    }
}
