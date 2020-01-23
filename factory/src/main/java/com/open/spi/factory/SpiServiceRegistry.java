package com.open.spi.factory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.open.spi.common.exception.MethodExecuteException;
import com.open.spi.common.exception.MethodNotFoundException;
import com.open.spi.factory.annotation.SpiService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public enum SpiServiceRegistry {

    INSTANCE;

    private Map<ServiceName, ServiceValue> services = new HashMap<ServiceName, ServiceValue>();

    private LocalDateTime startedTime = null;

    private SpiServiceRegistry() {

    }

    public ServiceValue getService(String interfaceName, String methodName, String parameterTypes, String version) {
        return services.get(new ServiceName(interfaceName, methodName, parameterTypes, version));
    }

    public List<ServiceName> queryAllService() {
        return services.keySet().stream().collect(Collectors.toList());
    }


    public List<ServiceName> queryService(String interfaceName, String methodName, String parameterTypes, String version) {
        return services.keySet().stream().filter(n -> (n.version.toLowerCase().contains(version.toLowerCase()) && n.getInterfaceName().toLowerCase().contains(interfaceName.toLowerCase()) && n.parameterTypes.toLowerCase().contains(parameterTypes.toLowerCase()) && n.methodName.toLowerCase().contains(methodName.toLowerCase()))).collect(Collectors.toList());
    }


    public Object executeService(String interfaceName, String methodName, String parameterTypes, String version, JSONArray args) throws MethodNotFoundException, MethodExecuteException {
        ServiceValue serviceValue = getService(interfaceName, methodName, parameterTypes, version);
        if (null == serviceValue) {
            log.error("spi service {} {}  {} {} not found:", interfaceName, methodName, parameterTypes, version);
            throw MethodNotFoundException.INSTANCE;
        }
        try {
            return serviceValue.invoke(args);
        } catch (Exception ex) {
            log.error("spi service {} {}  {} {}  execute {} fail:", interfaceName, methodName, parameterTypes, version, JSON.toJSONString(args));
            if (ex instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) ex;
            }
            MethodExecuteException executeException = new MethodExecuteException();
            executeException.setMessage(ex.getMessage());
            throw executeException;
        }
    }

    public void init(Collection<Object> set) {
        if (null != startedTime) {
            return;
        }
        if (CollectionUtils.isEmpty(set)) {
            log.warn("no spi service found");
            return;
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        set.stream().forEach(o -> addService(o));
        startedTime = LocalDateTime.now();
        stopWatch.stop();
        log.info("register spi service {}  used {} seconds", set.size(), stopWatch.getTime(TimeUnit.SECONDS));
    }

    //todo 优化
    public void addService(Object object) {
        Class clazz = object.getClass();
        SpiService spiService = AnnotationUtils.findAnnotation(clazz, SpiService.class);
        Class[] interfaceClazz = clazz.getInterfaces();
        val methods = clazz.getDeclaredMethods();
        Arrays.stream(interfaceClazz).forEach(n -> {
                    Method[] imethods = n.getDeclaredMethods();
                    if (imethods != null) {
                        Arrays.stream(imethods).filter(in -> {
                            return Arrays.stream(methods).anyMatch(m -> isOverride(in, m));
                        }).forEach(m -> {
                            val interfaceName = n.getCanonicalName();
                            val methodName = m.getName();
                            val parameterTypes = Arrays.stream(m.getParameterTypes()).map(Class::getCanonicalName).collect(Collectors.joining(","));
                            val version = spiService.version();
                            ServiceName serviceName = new ServiceName(interfaceName, methodName, parameterTypes, version);
                            if (services.get(serviceName) != null) {
                                log.error("spi service {} {}  {} {}  has already in spi factory, load fail", interfaceName, methodName, parameterTypes, version);
                                System.exit(Integer.MIN_VALUE);
                            }
                            log.info("load spi service {}.{}({}).{} successfully", interfaceName, methodName, parameterTypes, version);
                            services.put(serviceName, new ServiceValue(m, object));
                        });
                    }
                }
        );
    }

    private boolean isOverride(Method interfaceMethod, Method overrideMethod) {
        if (interfaceMethod == overrideMethod) {
            return true;
        }
        if (!interfaceMethod.getDeclaringClass().isAssignableFrom(overrideMethod.getDeclaringClass())) {
            return false;
        }
        if (!interfaceMethod.getName().equals(overrideMethod.getName())) {
            return false;
        }
        if (!interfaceMethod.getReturnType().isAssignableFrom(overrideMethod.getReturnType()))
            return false;
        Class[] classes = interfaceMethod.getParameterTypes();
        Class[] otherClasses = overrideMethod.getParameterTypes();
        if (classes == null && otherClasses == null) {
            return true;
        }
        if (classes == null || otherClasses == null || classes.length != otherClasses.length) {
            return false;
        }
        for (int index = 0; index < classes.length; index++) {
            if (classes[index] != otherClasses[index]) {
                return false;
            }
        }
        return true;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"interfaceName", "methodName", "parameterTypes", "version"})
    private class ServiceName {

        private final String interfaceName;

        private final String methodName;

        private final String parameterTypes;

        private final String version;

    }

    @AllArgsConstructor
    private class ServiceValue {

        private final Method method;

        private final Object object;

        public Object invoke(JSONArray args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            Class[] argumentTypes = method.getParameterTypes();
            if (argumentTypes.length != args.size()) {
                throw new IllegalArgumentException();
            }
            Object[] params = new Object[argumentTypes.length];
            for (int index = 0; index < argumentTypes.length; index++) {
                params[index] = args.getObject(index, argumentTypes[index]);
            }
            return method.invoke(object, params);
        }

    }


}
