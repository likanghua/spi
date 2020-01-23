package com.open.spi.common.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.objenesis.strategy.StdInstantiatorStrategy;

public class KryoUtil {

    private static final ThreadLocal<Kryo> KRYOLOCAL = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            return kryo;
        }
    };

    private KryoUtil() {

    }

    public static byte[] writeClassAndObject(Object object) {
        if (Objects.isNull(object)) {
            return null;
        }
        try (Output output = new Output(32, -1)) {
            KRYOLOCAL.get().writeClassAndObject(output, object);
            return output.toBytes();
        }


    }

    public static <T> T readObject(byte[] data, Class<T> clazz) {
        if (ArrayUtils.isEmpty(data)) {
            return null;
        }
        try (Input input = new Input(data)) {
            return KRYOLOCAL.get().readObject(input, clazz);
        }
    }

    public static <T> byte[] writeObject(T object) {
        if (Objects.isNull(object)) {
            return null;
        }
        try (Output output = new Output(32, -1)) {
            KRYOLOCAL.get().writeObject(output, object);
            return output.toBytes();
        }
    }


    public static Object readClassAndObject(byte[] data) {
        if (ArrayUtils.isEmpty(data)) {
            return null;
        }
        try (Input input = new Input(data)) {
            return KRYOLOCAL.get().readClassAndObject(input);
        }

    }


}
