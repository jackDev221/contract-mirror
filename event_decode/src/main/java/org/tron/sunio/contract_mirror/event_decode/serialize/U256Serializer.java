package org.tron.sunio.contract_mirror.event_decode.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.generated.Uint256;

import java.lang.reflect.Type;
import java.math.BigInteger;

public class U256Serializer implements JsonSerializer<Uint256>, JsonDeserializer<Uint256> {
    @Override
    public Uint256 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return new Uint256(new BigInteger(json.getAsString(), 16));
    }

    @Override
    public JsonElement serialize(Uint256 src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(TypeEncoder.encode(src));
    }
}
