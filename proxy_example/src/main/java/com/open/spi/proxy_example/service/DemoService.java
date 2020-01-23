package com.open.spi.proxy_example.service;

import com.open.spi.common.util.RestResponse;
import com.open.spi.proxy_example.dto.Person;

public interface DemoService {

    public RestResponse<Person> getPerson(Person person);

}
