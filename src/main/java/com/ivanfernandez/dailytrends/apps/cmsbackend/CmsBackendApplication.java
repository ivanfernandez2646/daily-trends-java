package com.ivanfernandez.dailytrends.apps.cmsbackend;

import com.ivanfernandez.dailytrends.apps.cmsbackend.config.DailyTrendsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DailyTrendsProperties.class)
public class CmsBackendApplication {

    static void main(String[] args) {
        SpringApplication.run(CmsBackendApplication.class, args);
    }
}
