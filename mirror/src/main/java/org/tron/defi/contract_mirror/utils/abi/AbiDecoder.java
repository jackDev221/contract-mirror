package org.tron.defi.contract_mirror.utils.abi;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tron.common.utils.AddressUtil;
import org.tron.common.utils.ByteArray;

import java.math.BigInteger;

import static org.tron.defi.contract_mirror.utils.abi.Constants.*;

public class AbiDecoder {
    public static ImmutablePair<String, Integer> DecodeAddress(String buffer) {
        byte[] tronAddress = AddressConverter.EthToTronAddress(ByteArray.fromHexString(buffer));
        String base58Address = AddressUtil.encode58Check(tronAddress);
        return new ImmutablePair<>(base58Address, PADDING_BYTES_STRING_SIZE);
    }

    public static ImmutablePair<BigInteger, Integer> DecodeNumber(String buffer) {
        if (buffer == null || buffer.length() < PADDING_BYTES_STRING_SIZE) {
            throw new IllegalArgumentException("INVALID BUFFER LENGTH");
        }
        BigInteger value = new BigInteger(buffer.substring(0, PADDING_BYTES_STRING_SIZE), HEX_BASE);
        return new ImmutablePair<>(value, PADDING_BYTES_STRING_SIZE);
    }

    public static ImmutablePair<String, Integer> DecodeString(String buffer) {
        ImmutablePair<BigInteger, Integer> result = DecodeNumber(buffer);
        BigInteger length = result.left;
        int start = result.right.intValue();
        int end = start + length.intValue() * 2;
        if (end > buffer.length()) {
            throw new IllegalArgumentException("INVALID BUFFER LENGTH");
        }
        String decoded = ByteArray.toStr(ByteArray.fromHexString(buffer.substring(start, end)));
        return new ImmutablePair<>(decoded, end);
    }

    public static String DecodeStringFromTuple(String buffer, int index) {
        ImmutablePair<BigInteger, Integer> result = DecodeNumber(buffer.substring(
            PADDING_BYTES_STRING_SIZE * index));
        int offset = result.left.intValue() * BYTE_STRING_SIZE;
        if (offset >= buffer.length()) {
            throw new IllegalArgumentException("INVALID BUFFER LENGTH");
        }
        return DecodeString(buffer.substring(offset)).left;
    }
}
