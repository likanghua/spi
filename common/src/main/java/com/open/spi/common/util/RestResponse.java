package com.open.spi.common.util;

public class RestResponse<T> {

    private final static String SUCCCESS_CODE = "0";

    private String code = "0";

    private String message = "操作成功";

    private Object[] args;

    private T data;

    private RestResponse(T data) {
        this.data = data;
    }

    private RestResponse() {
    }

    private RestResponse(String code, String message, Object[] args) {
        this.code = code;
        this.message = message;
        this.args = args;
    }

    public static RestResponse ok() {
        return new RestResponse();
    }

    public static <T> RestResponse ok(T data) {
        return new RestResponse(data);
    }

    public static RestResponse fail(String code, String message, Object... args) {
        return new RestResponse(code, message, args);
    }

    public boolean isSuccess() {
        return SUCCCESS_CODE.equals(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

}
