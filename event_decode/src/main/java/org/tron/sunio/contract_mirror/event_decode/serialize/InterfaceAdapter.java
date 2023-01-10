package org.tron.sunio.contract_mirror.event_decode.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.HashMap;


@Slf4j
public class InterfaceAdapter<T> implements JsonSerializer, JsonDeserializer {

    private static final String CLASSNAME = "CLASS";
    private static final String TYPE = "type";

    private static final HashMap<String, Class> classes = new HashMap<>(32);

    public static void addClass(String type, Class cls) {
        classes.put(type, cls);
    }

    public static void addClass(Class cls) {
        classes.put(cls.getSimpleName(), cls);
    }

    public T deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Class cls;
        if (jsonObject.get(TYPE) != null && !jsonObject.get(TYPE).isJsonNull() && classes.containsKey(jsonObject.get(TYPE).getAsString())) {
            cls = classes.get(jsonObject.get(TYPE).getAsString());
        } else {
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
            String className = prim.getAsString();
            cls = getObjectClass(className);
        }
        return jsonDeserializationContext.deserialize(jsonObject, cls);
    }

    public JsonElement serialize(
            Object jsonElement, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject data = (JsonObject) jsonSerializationContext.serialize(jsonElement);
        if (data.get(TYPE) == null || data.get(TYPE).isJsonNull()) {
            data.addProperty(TYPE, jsonElement.getClass().getSimpleName());
        }
        if (!classes.containsKey(data.get(TYPE).getAsString())) {
            data.addProperty(CLASSNAME, jsonElement.getClass().getName());
        }
        return data;
    }

    /****** Helper method to get the className of the object to be deserialized *****/
    public Class getObjectClass(String className) {
        try {
            Class c;
            c = Class.forName(className);
            return c;
        } catch (ClassNotFoundException e) {
            log.warn(e.getMessage(), e);
            throw new JsonParseException(e.getMessage());
        }
    }
}
