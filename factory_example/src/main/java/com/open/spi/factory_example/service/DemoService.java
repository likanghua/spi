package com.open.spi.factory_example.service;

import com.open.spi.common.util.RestResponse;
import com.open.spi.factory.annotation.SpiService;
import com.open.spi.proxy_example.dto.Person;


@SpiService
public class DemoService implements com.open.spi.proxy_example.service.DemoService {

    @Override
    public RestResponse<Person> getPerson(Person person) {
        System.err.println(person.getName() + "*****************");
        return RestResponse.ok(person);
    }

}
