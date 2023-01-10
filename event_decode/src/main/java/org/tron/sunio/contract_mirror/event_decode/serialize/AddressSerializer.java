package org.tron.sunio.contract_mirror.event_decode.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.web3j.abi.datatypes.Address;

import java.lang.reflect.Type;

public class AddressSerializer implements JsonSerializer<Address>, JsonDeserializer<Address> {
    @Override
    public Address deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return new Address(json.getAsString());
    }

    @Override
    public JsonElement serialize(Address src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
