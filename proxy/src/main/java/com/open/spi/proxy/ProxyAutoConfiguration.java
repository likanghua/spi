package com.open.spi.proxy;

import com.open.spi.proxy.annotation.SpiProxy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Duration;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@EnableConfigurationProperties(ProxyProperties.class)
@AutoConfigureAfter(org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.class)
@ConditionalOnBean({RestTemplateBuilder.class, ProxyContextService.class})
@Configuration
@EnableWebMvc
@Slf4j
public class ProxyAutoConfiguration {

    public ProxyAutoConfiguration(ProxyProperties proxyProperties, RestTemplateBuilder restTemplateBuilder, ApplicationContext applicationContext) {
        Assert.notNull(proxyProperties, "must have proxy properties");
        Assert.notEmpty(proxyProperties.getConfig(), "config cannot be empty");
        proxyProperties.getConfig().forEach((k, v) -> {
            Assert.hasLength(v.getBaseUrl(), "baseUrl cannot be empty");
            Assert.notNull(v.getConnectionRequestTimeout(), "connectionRequestTimeout cannot be null");
            Assert.notNull(v.getReadTimeout(), "readTimeout cannot be null");
            Assert.notNull(v.getConnectTimeout(), "connectTimeout cannot be null");
            BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(RestTemplateFactoryBean.class);
            beanDefinitionBuilder.addConstructorArgValue(restTemplate(restTemplateBuilder, v));
            BeanDefinition rawBeanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
            rawBeanDefinition.setScope(SCOPE_SINGLETON);
            beanFactory.registerBeanDefinition(k, rawBeanDefinition);

        });
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(ProxyContextService proxyContextService) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        ProxyContext.INSTANCE.setValue(proxyContextService.getContext(request));
                        return true;
                    }

                    @Override
                    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
                        ProxyContext.INSTANCE.clear();
                    }
                });
            }
        };
    }

    @Bean
    public BeanPostProcessor beanPostProcessor(ApplicationContext applicationContext, ProxyProperties proxyProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                Class<?> targetCls = bean.getClass();
                if (AopUtils.isAopProxy(bean)) {
                    targetCls = AopUtils.getTargetClass(bean);
                }
                FieldUtils.getFieldsListWithAnnotation(targetCls, SpiProxy.class).stream().forEach(f -> {
                    SpiProxy spiProxy = f.getDeclaredAnnotation(SpiProxy.class);
                    String version = StringUtils.trimToEmpty(spiProxy.version());
                    f.setAccessible(true);
                    try {
                        FieldUtils.writeField(f, bean, Proxy.newProxyInstance(
                                this.getClass().getClassLoader(),
                                new Class[]{f.getType()},
                                new RemoteServiceProxy(applicationContext, proxyProperties, version)
                        ));
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        System.exit(Integer.MIN_VALUE);
                    }
                });
                return bean;
            }
        };
    }

    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder, ProxyProperties.Config config) {
        return restTemplateBuilder.requestFactory(() -> {
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            if (null != config.getMaxTotal()) {
                connectionManager.setMaxTotal(config.getMaxTotal());
                connectionManager.setDefaultMaxPerRoute(config.getMaxTotal());
            }
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                    .setConnectionManager(connectionManager)
                    .setRequestExecutor(new HttpRequestExecutor() {
                        @Override
                        public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context) throws IOException, HttpException {
                            preProcess(request, new HttpProcessor() {
                                @Override
                                public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                                    //todo 此次加签最优
                                }

                                @Override
                                public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {

                                }
                            }, context);
                            return super.execute(request, conn, context);
                        }
                    });
            if (null != config.getRetryCount()) {
                httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(config.getRetryCount(), true));
            }
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClientBuilder.build());
            requestFactory.setConnectionRequestTimeout(config.getConnectionRequestTimeout());
            return requestFactory;
        }).errorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }

        }).setReadTimeout(Duration.ofMillis(config.getReadTimeout())).setConnectTimeout(Duration.ofMillis(config.getConnectTimeout())).build();

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RestTemplateFactoryBean implements FactoryBean<RestTemplate> {

        private RestTemplate restTemplate;

        @Override
        public RestTemplate getObject() throws BeansException {
            return restTemplate;
        }

        @Override
        public Class<?> getObjectType() {
            return RestTemplate.class;
        }

    }

}
