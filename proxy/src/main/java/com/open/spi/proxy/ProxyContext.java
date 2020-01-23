package com.open.spi.proxy;

public enum ProxyContext {
    INSTANCE;

    private final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public String getValue() {
        return CONTEXT.get();
    }

    public void setValue(String value) {
        CONTEXT.set(value);
    }

    public void clear() {
        CONTEXT.remove();
    }
}
