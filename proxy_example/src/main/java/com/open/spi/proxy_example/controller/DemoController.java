package com.open.spi.proxy_example.controller;

import com.open.spi.common.util.RestResponse;
import com.open.spi.proxy.annotation.SpiProxy;
import com.open.spi.proxy_example.dto.Person;
import com.open.spi.proxy_example.service.DemoService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @SpiProxy
    private DemoService demoService;

    @GetMapping
    public RestResponse<Person> demo() {
        return demoService.getPerson(new Person(0, "hello world"));
    }

}
