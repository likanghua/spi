package com.open.spi.proxy;

import javax.servlet.http.HttpServletRequest;

public interface ProxyContextService {

    public String getContext(HttpServletRequest httpServletRequest);

}
