package org.tron.sunio.contract_mirror.mirror.tools;

import org.tron.sunio.contract_mirror.event_decode.utils.GsonUtil;

public class DeepCopyUtils {
    public static <T> T deepCopy(T input, Class<T> classOfT) {
        String jsonString = GsonUtil.objectToGson(input);
        return GsonUtil.gsonToObject(jsonString, classOfT);
    }
}
