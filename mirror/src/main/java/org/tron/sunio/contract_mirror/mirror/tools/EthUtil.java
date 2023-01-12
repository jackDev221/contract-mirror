package org.tron.sunio.contract_mirror.mirror.tools;

import com.google.common.base.Strings;

public class EthUtil {
    public static final String ZERO = "0x0000000000000000000000000000000000000000";
    public static final String HEX_PREFIX = "0x";

    public static boolean hasPrefix(String input) {
        return input.startsWith(HEX_PREFIX);
    }

    public static String addHexPrefix(String input) {
        if (Strings.isNullOrEmpty(input))
            return "";
        if (input.startsWith(HEX_PREFIX))
            return input;
        else
            return HEX_PREFIX + input;
    }

}
