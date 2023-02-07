package org.tron.sunio.contract_mirror.event_decode.events;

import cn.hutool.core.util.ObjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int128;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventUtils {
    private static final String EVENT_START = "event";
    private static final String EVENT_INDEXED = "indexed";
    public static final String HEX_PREFIX = "0x";

    public static EventValues getEventValue(String eventBody, List<String> topics, String data, boolean checkSign) {
        Event event = parseEventString(eventBody);
        if (ObjectUtil.isNull(event)) {
            return null;
        }
        if (checkSign) {
            String encodedEventSignature = encodedEventSignature(event);
            if (!topics.get(0).equals(encodedEventSignature)) {
                return null;
            }
        }
        List<Type> indexedValues = new ArrayList<>();
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                data, event.getNonIndexedParameters());
        List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
        for (int i = 0; i < indexedParameters.size(); i++) {
            Type value = FunctionReturnDecoder.decodeIndexedValue(
                    topics.get(i + 1), indexedParameters.get(i));
            indexedValues.add(value);
        }
        return new EventValues(indexedValues, nonIndexedValues);
    }

    public static Event parseEventString(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }
        if (!input.startsWith(EVENT_START)) {
            return null;
        }
        String[] result = input.split(EVENT_START)[1].split("\\(");
        String name = result[0].strip();
        String functionStr = result[1].strip().split("\\)")[0].strip();
        String[] params = functionStr.split(",");
        List<TypeReference<?>> parameters = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            TypeReference<?> paramRef = parseParamString(params[i]);
            if (paramRef != null) {
                parameters.add(paramRef);
            }
        }
        return new Event(name, parameters);
    }

    public static String encodedEventSignature(Event event) {
        if (ObjectUtil.isNull(event)) {
            return null;
        }
        return EventEncoder.encode(event);
    }

    public static void genSigNameMap(Map<String, String> sigMap, Map<String, String> eventInfoMap) {
        for (Map.Entry<String, String> entry : eventInfoMap.entrySet()) {
            String sig = EventUtils.encodedEventSignature(EventUtils.parseEventString(entry.getValue()));
            if (ObjectUtil.isNotNull(sig)) {
                sigMap.put(removePrefix(sig), entry.getKey());
            }
        }
    }

    public static String removePrefix(String input) {
        if (StringUtils.isNotEmpty(input) && input.startsWith(HEX_PREFIX)) {
            return input.substring(2);
        }

        return input;
    }

    private static TypeReference<?> parseParamString(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        String[] infos = input.strip().split(" ");
        String paramType = infos[0];
        boolean indexed = false;
        for (String info : infos) {
            if (info.equals(EVENT_INDEXED)) {
                indexed = true;
                break;
            }
        }
        return getTypeReference(paramType, indexed);
    }

    private static TypeReference<?> getTypeReference(String paramType, boolean indexed) {
        // 其他类型之后看需要再添加
        if (StringUtils.isEmpty(paramType)) {
            return null;
        }
        paramType = paramType.strip();
        TypeReference<?> result = null;
        switch (paramType) {
            case "address":
                result = new TypeReference<Address>(indexed) {
                };
                break;
            case "uint256":
                result = new TypeReference<Uint256>(indexed) {
                };
                break;
            case "uint112":
                result = new TypeReference<Uint112>(indexed) {
                };
                break;
            case "uint64":
                result = new TypeReference<Uint64>(indexed) {
                };
                break;
            case "uint":
                result = new TypeReference<Uint>(indexed) {
                };
                break;
            case "uint256[2]":
                result = new TypeReference.StaticArrayTypeReference<StaticArray<Uint256>>(2) {
                };
                break;
            case "uint256[3]":
                result = new TypeReference.StaticArrayTypeReference<StaticArray<Uint256>>(3) {
                };
                break;
            case "uint256[4]":
                result = new TypeReference.StaticArrayTypeReference<StaticArray<Uint256>>(4) {
                };
                break;
            case "int128":
                result = new TypeReference<Int128>(indexed) {
                };
                break;
            case "bytes32":
                result = new TypeReference<Bytes32>(indexed) {
                };
                break;
        }
        return result;
    }
}
