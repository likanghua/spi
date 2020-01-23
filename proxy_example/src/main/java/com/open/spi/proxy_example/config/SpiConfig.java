package com.open.spi.proxy_example.config;

import com.open.spi.proxy.ProxyContextService;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpiConfig {


    @Bean
    public ProxyContextService proxyContextService() {
        return new ProxyContextService() {
            @Override
            public String getContext(HttpServletRequest httpServletRequest) {
                return "likanghua";
            }
        };
    }

}
