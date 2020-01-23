package com.open.spi.proxy;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(ProxyProperties.PREFIX)
@Configuration
public class ProxyProperties {

    public static final String PREFIX = "proxy";

    private Map<String, Config> config;


    @Getter
    @Setter
    public static class Config {

        private String baseUrl;

        private Integer readTimeout;

        private Integer connectTimeout;

        private Integer connectionRequestTimeout;

        private Integer maxTotal;

        private Integer retryCount;

    }

}
