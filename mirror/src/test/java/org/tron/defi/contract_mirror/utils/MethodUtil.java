package org.tron.defi.contract_mirror.utils;

import java.lang.reflect.Method;

public class MethodUtil {
    public static Method getNonAccessibleMethod(Class<?> clz,
                                                String methodName,
                                                Class<?>... parameterType) throws NoSuchMethodException {
        Method method = clz.getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method;
    }
}
