package com.gwf.xunwu;

import com.gwf.xunwu.repository.HouseRepository;
import com.gwf.xunwu.repository.HouseTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

/**
 * 启动类
 * @author gaowenfeng
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
            SpringApplication.run(Application.class,args);
    }
}
