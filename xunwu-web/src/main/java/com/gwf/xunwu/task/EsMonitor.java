package com.gwf.xunwu.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author gaowenfeng
 */
@Component
@Slf4j
public class EsMonitor {
    private static final String HEALTH_CHECK_API = "http://localhost:9200/_cluster/health";
    private static final String GREEN = "green";
    private static final String YELLOW = "yellow";
    private static final String RED = "red";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JavaMailSender javaMailSender;

    @Scheduled(fixedDelay = 5000)
    public void healthCheck(){
        HttpGet get = new HttpGet(HEALTH_CHECK_API);
        HttpClient client = HttpClients.createDefault();
        try {
            HttpResponse response = client.execute(get);
            if(HttpServletResponse.SC_OK != response.getStatusLine().getStatusCode()){
                log.warn("Can not access Es Service normally ! please check the server.");
            }else {
                String body = EntityUtils.toString(response.getEntity(),"UTF-8");
                JsonNode jsonNode = objectMapper.readTree(body);
                String status = jsonNode.get("status").asText();
                String message;
                boolean isNormal = false;
                switch (status){
                    case GREEN:
                        message = "Es server run normally";
                        isNormal = true;
                        break;
                    case YELLOW:
                        message = "Es server gets status yellow! please check the server.";
                        break;
                    case RED:
                        message = "Es server get status RED!!! Must check th server. ";
                        break;
                    default:
                        message = String.format("Unknown ES status from server {},please check it",status);
                        break;
                }

                if(!isNormal){
                    sendAlertMessage(message);
                }

                int totalNode = jsonNode.get("number_of_nodes").asInt();
                if(totalNode<1){
                    sendAlertMessage("es节点丢失");
                }
            }
        } catch (IOException e) {
            log.error("httpclient执行异常",e);
        }
    }

    private void sendAlertMessage(String message){
        SimpleMailMessage simpleMailMessage =new SimpleMailMessage();
        simpleMailMessage.setFrom("17602686137@163.com");
        simpleMailMessage.setTo("17602686137@163.com");
        simpleMailMessage.setSubject("Es邮件报警");
        simpleMailMessage.setText(message);

        javaMailSender.send(simpleMailMessage);
    }
}
