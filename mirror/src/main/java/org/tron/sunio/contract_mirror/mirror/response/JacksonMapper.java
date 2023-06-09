package org.tron.sunio.contract_mirror.mirror.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonMapper extends ObjectMapper {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(JacksonMapper.class);
    private static JacksonMapper mapper;

    private JacksonMapper() {
        this(JsonInclude.Include.NON_EMPTY);
    }

    private JacksonMapper(JsonInclude.Include include) {
        // 设置输出时包含属性的风格
        if (include != null) {
            this.setSerializationInclusion(include);
        }
        // 允许单引号、允许不带引号的字段名称
        this.enableSimple();
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        this.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 运行empty的属性
        this.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 空值处理为空串
        this.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object value,
                                  JsonGenerator jgen,
                                  SerializerProvider provider) throws IOException, JsonProcessingException {
                jgen.writeString("");
            }
        });
        //this.registerModule(new SimpleModule().addSerializer(new MyBigDecimalDesirializer()));
        // 进行HTML解码。
         /*this.registerModule(new SimpleModule().addSerializer(String.class, new JsonSerializer<String>(){
             @Override
             public void serialize(String value, JsonGenerator jgen,
                     SerializerProvider provider) throws IOException,
                     JsonProcessingException {
                 jgen.writeString(StringEscapeUtils.unescapeHtml4(value));
             }
         }));  */
        // 设置时区
        this.setTimeZone(TimeZone.getDefault());//getTimeZone("GMT+8:00")
    }

    /**
     * 使用枚举的toString函数来读写Enum,
     * 为False时使用Enum的name()函数来 读写Enum, 默认为False.
     * 注意本函数一定要在Mapper创建后, 所有的读写动作之前调用.
     */
    public JacksonMapper enableEnumUseToString() {
        this.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        this.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        return this;
    }


    /**
     * 允许单引号
     * 允许不带引号的字段名称
     */
    public JacksonMapper enableSimple() {
        this.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        this.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        this.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        return this;
    }

    /**
     * 创建只输出非Null且非Empty(如List.isEmpty)的属性到Json字符串的Mapper,建议在外部接口中使用.
     */
    public static JacksonMapper getInstance() {
        if (mapper == null) {
            mapper = new JacksonMapper(JsonInclude.Include.ALWAYS).enableSimple();
        }
        return mapper;
    }

    /**
     * 创建只输出初始值被改变的属性到Json字符串的Mapper, 最节约的存储方式，建议在内部接口中使用。
     */
    public static JacksonMapper nonDefaultMapper() {
        if (mapper == null) {
            mapper = new JacksonMapper(JsonInclude.Include.NON_DEFAULT);
        }
        return mapper;
    }

    /**
     * 取出Mapper做进一步的设置或使用其他序列化API.
     */
    public ObjectMapper getMapper() {
        return this;
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。
     * 如果对象为Null, 返回"null".
     * 如果集合为空集合, 返回"[]".
     */
    public String toJson(Object object) {
        try {
            return this.writeValueAsString(object);
        } catch (IOException e) {
            logger.warn("write to json string error:" + object, e);
            return null;
        }
    }

    /**
     * 反序列化POJO或简单Collection如List<String>.
     * <p>
     * 如果JSON字符串为Null或"null"字符串, 返回Null.
     * 如果JSON字符串为"[]", 返回空集合.
     * <p>
     * 如需反序列化复杂Collection如List<MyBean>, 请使用fromJson(String,JavaType)
     *
     * @see #fromJson(String, JavaType)
     */
    public <T> T fromJson(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            return this.readValue(jsonString, clazz);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 反序列化复杂Collection如List<Bean>, 先使用函数createCollectionType构造类型,然后调用本函数.
     *
     * @see #createCollectionType(Class, Class...)
     */
    @SuppressWarnings("unchecked")
    public <T> T fromJson(String jsonString, JavaType javaType) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            return (T) this.readValue(jsonString, javaType);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 构造泛型的Collection Type如:
     * ArrayList<MyBean>, 则调用constructCollectionType(ArrayList.class,MyBean.class)
     * HashMap<String,MyBean>, 则调用(HashMap.class,String.class, MyBean.class)
     */
    public JavaType createCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return this.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    /**
     * 当JSON里只含有Bean的部分属性时，更新一个已存在Bean，只覆盖该部分的属性.
     */
    @SuppressWarnings("unchecked")
    public <T> T update(String jsonString, T object) {
        try {
            return (T) this.readerForUpdating(object).readValue(jsonString);
        } catch (JsonProcessingException e) {
            logger.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        }
        return null;
    }

    /**
     * 输出JSONP格式数据.
     */
    public String toJsonP(String functionName, Object object) {
        return toJson(new JSONPObject(functionName, object));
    }


    /**
     * 对象转换为JSON字符串
     *
     * @param object
     * @return
     */
    public static String toJsonString(Object object) {
        return JacksonMapper.getInstance().toJson(object);
    }

    /**
     * JSON字符串转换为对象
     *
     * @param jsonString
     * @param clazz
     * @return
     */
    public static Object fromJsonString(String jsonString, Class<?> clazz) {
        return JacksonMapper.getInstance().fromJson(jsonString, clazz);
    }
}
