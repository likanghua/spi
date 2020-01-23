package com.open.spi.proxy;

import com.alibaba.fastjson.JSON;
import com.open.spi.common.exception.BaseException;
import com.open.spi.common.exception.ConfigNotFoundException;
import com.open.spi.common.exception.ExceptionResponse;
import com.open.spi.common.exception.MethodNotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
public class RemoteServiceProxy implements InvocationHandler {

    private ApplicationContext applicationContext;

    private ProxyProperties proxyProperties;

    private String version;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String value = ProxyContext.INSTANCE.getValue();
        RestTemplate restTemplate = null;
        try {
            restTemplate = applicationContext.getBean(value, RestTemplate.class);
        } catch (Exception ex) {
            log.error("get bean fail:{}", ex.getMessage());
        }
        Class<?> returnType = method.getReturnType();
        if (null == restTemplate) {
            log.error("spi config {} not found, you must config it in spring boot config file", value);
            throw ConfigNotFoundException.INSTANCE;
        }

        String url = proxyProperties.getConfig().get(value).getBaseUrl();
        url = url.endsWith("/") ? url : url.concat("/");
        url = url.concat("service");
        ResponseEntity<String> responseEntity = restTemplate.exchange(RequestEntity.post(URI.create(url)).header("version", version).header("parameter_types", Arrays.stream(method.getParameterTypes()).map(Class::getCanonicalName).collect(Collectors.joining(","))).header("class_name", method.getDeclaringClass().getCanonicalName()).header("method_name", method.getName()).body(args), String.class);
        if (!responseEntity.getStatusCode().isError()) {
            return JSON.parseObject(responseEntity.getBody(), returnType);
        } else {
            ExceptionResponse exceptionResponse = JSON.parseObject(responseEntity.getBody(), ExceptionResponse.class);
            String exceptionClass = exceptionResponse.getException();
            Class<?> exception = null;
            try {
                exception = (Class<? extends Exception>) ClassUtils.forName(exceptionClass, getClass().getClassLoader());
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw ex;
            }
            if (MethodNotFoundException.class == exception) {
                throw MethodNotFoundException.INSTANCE;
            }
            if (BaseException.class.isAssignableFrom(exception)) {
                BaseException baseException = null;
                try {
                    baseException = (BaseException) exception.newInstance();
                } catch (Exception ex) {
                    throw ex;

                }
                baseException.setMessage(StringUtils.trimToEmpty(exceptionResponse.getMessage()));
                throw baseException;
            }
            Constructor constructor = null;
            try {
                constructor = exception.getDeclaredConstructor(String.class);
            } catch (Exception ex) {

            }
            if (null != constructor) {
                throw (Exception) constructor.newInstance(StringUtils.trimToEmpty(exceptionResponse.getMessage()));
            }
            try {
                constructor = exception.getDeclaredConstructor();
            } catch (Exception ex) {

            }
            if (null != constructor) {
                throw (Exception) constructor.newInstance();
            }
            throw new Exception();
        }


    }

}
