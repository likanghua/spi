package com.open.spi.common.exception;

public class ConfigNotFoundException extends BaseException {

    public static final ConfigNotFoundException INSTANCE = new ConfigNotFoundException();

    private ConfigNotFoundException() {

    }


}
