package com.open.spi.factory;

import com.alibaba.fastjson.JSONArray;
import com.open.spi.common.exception.MethodExecuteException;
import com.open.spi.common.exception.MethodNotFoundException;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/service")
public class SpiServiceEndPoint {

    //todo 是否需要传多个data
    @PostMapping
    public Object executeSpi(@RequestBody JSONArray arguments, @RequestHeader(value = "version") String version, @RequestHeader("class_name") String className, @RequestHeader("method_name") String methodName, @RequestHeader("parameter_types") String parameter_types) throws MethodExecuteException, MethodNotFoundException, IllegalArgumentException {
        return SpiServiceRegistry.INSTANCE.executeService(className, methodName, StringUtils.trimAllWhitespace(parameter_types), org.apache.commons.lang3.StringUtils.trimToEmpty(version), arguments);

    }

}
