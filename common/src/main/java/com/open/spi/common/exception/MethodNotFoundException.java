package com.open.spi.common.exception;

public class MethodNotFoundException extends BaseException {

    public static final MethodNotFoundException INSTANCE = new MethodNotFoundException();

    private MethodNotFoundException() {
    }


}
