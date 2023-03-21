package org.tron.sunio.contract_mirror.event_decode.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.math.BigInteger;

public class BigIntegerSerializer implements JsonSerializer<BigInteger>, JsonDeserializer<BigInteger> {
    @Override
    public BigInteger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return new BigInteger(json.getAsString(), 10);
    }

    @Override
    public JsonElement serialize(BigInteger src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
