package com.open.spi.factory;

import com.open.spi.factory.annotation.SpiService;
import com.open.spi.factory.exception_advice.ExceptionAdvice;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

@Slf4j
@Import({SpiServiceEndPoint.class, ExceptionAdvice.class})
@Configuration
public class SpiFactoryAutoConfiguration implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        log.info("starting register spi service");
        Map<String, Object> beans = applicationStartedEvent.getApplicationContext().getBeansWithAnnotation(SpiService.class);
        if (!CollectionUtils.isEmpty(beans)) {
            SpiServiceRegistry.INSTANCE.init(beans.values());
        }
        log.info("register spi service successfully");

    }

}
