package org.tron.defi.contract_mirror.utils.abi;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Constants {
    public static final int HEX_BASE = 16;
    public static final String HEX = "0123456789abcdef";
    public static final Map<Byte, Integer> HEX_MAP;
    public static final int BYTE_STRING_SIZE = 2;
    public static final int PADDING_BYTES = 32;
    public static final int PADDING_BYTES_STRING_SIZE = PADDING_BYTES * BYTE_STRING_SIZE;

    static {
        Map<Byte, Integer> dummy = new ConcurrentHashMap<>();
        for (int i = 0; i < HEX.length(); i++) {
            dummy.put((byte) HEX.charAt(i), i);
        }
        HEX_MAP = Collections.unmodifiableMap(dummy);
    }
}
