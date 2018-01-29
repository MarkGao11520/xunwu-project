package com.gwf.xunwu.config;

import lombok.Data;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * @author gaowenfeng
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "es")
public class ElasticSearchConfig {

    private String clusterName;

    private List<Map<String,String>> address;


    @Bean
    public TransportClient esClient() {
        try {
            //设置settings，默认的cluster.name为elasticsearch
            Settings settings = Settings.builder()
                    .put("cluster.name",clusterName)
                    .put("client.transport.sniff", true)
                    .build();
            //创建客户端，如果使用默认配置，传参为Settings.EMPTY
            TransportClient client = new PreBuiltTransportClient(settings);

            //创建结点（可以根据情况创建多个结点或者一个结点)
            for(Map<String,String> map:address){
                InetSocketTransportAddress node = new InetSocketTransportAddress(
                        InetAddress.getByName(map.get("ip")),Integer.valueOf(map.get("port"))
                );
                client.addTransportAddress(node);
            }
            return client;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

}
