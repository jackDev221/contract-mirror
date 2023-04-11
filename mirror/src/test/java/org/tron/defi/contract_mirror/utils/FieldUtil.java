package org.tron.defi.contract_mirror.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

@Slf4j
public class FieldUtil {
    public static <T> T get(Object object, String fieldName) throws IllegalAccessException {
        Field field = FieldUtils.getField(object.getClass(), fieldName, true);
        return (T) field.get(object);
    }

    public static void set(Object object,
                           String fieldName,
                           Object value) throws IllegalAccessException {
        Field field = FieldUtils.getField(object.getClass(), fieldName, true);
        field.set(object, value);
        log.info("{} set {} = {}", object.getClass().getName(), fieldName, value);
    }
}
