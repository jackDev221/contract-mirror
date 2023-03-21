package org.tron.sunio.contract_mirror.event_decode.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.tron.sunio.contract_mirror.event_decode.serialize.AddressSerializer;
import org.tron.sunio.contract_mirror.event_decode.serialize.BigIntegerSerializer;
import org.tron.sunio.contract_mirror.event_decode.serialize.U256Serializer;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

import java.lang.reflect.Type;
import java.math.BigInteger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GsonUtil {

    private static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Address.class, new AddressSerializer());
        builder.registerTypeAdapter(Uint256.class, new U256Serializer());
        builder.registerTypeAdapter(BigInteger.class, new BigIntegerSerializer());
        GSON = builder.serializeNulls().create();
    }

    /**
     * POJO对象转json格式
     */
    public static String objectToGson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * json转成POJO对象
     */
    public static <T> T gsonToObject(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }

    /**
     * json转成POJO对象
     */
    public static <T> T gsonToObject(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * json转成Gson对象
     */
    public static JsonObject gsonToJsonObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * json转成Gson数组
     */
    public static JsonArray gsonToJsonArray(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    public static String addProperty(Object obj, String key, String value) {
        JsonElement jsonElement = GSON.toJsonTree(obj);
        jsonElement.getAsJsonObject().addProperty(key, value);
        return GSON.toJson(jsonElement);
    }
}
