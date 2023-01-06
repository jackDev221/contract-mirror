package org.tron.sunio.tronsdk;

import cn.hutool.core.codec.Base58;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.*;
import org.tron.sunio.tronsdk.common.crypto.Sha256Sm3Hash;
import org.tron.sunio.tronsdk.common.core.Parameter.CommonConstant;

import java.util.Arrays;

public class WalletUtil {

    //以太坊地址(20 bytes)的hex string形式
    public static String ethAddressHex(String base58check) {
        return HexUtil.encodeHexStr(ethAddress(base58check));
    }

    //以太坊地址(20 bytes)的byte数组形式
    public static byte[] ethAddress(String base58check) {
        return ArrayUtil.sub(decodeFromBase58Check(base58check), 1, 21);
    }

    //波场地址(21 bytes)的hex string形式
    public static String tronAddressHex(String base58check) {
        return HexUtil.encodeHexStr(tronAddress(base58check));
    }

    //波场地址(21 bytes)的byte数组形式
    public static byte[] tronAddress(String base58check) {
        return decodeFromBase58Check(base58check);
    }

    public static boolean isTronValidAddress(String tronAddress){
        if (!StrUtil.isNotBlank(tronAddress) || !tronAddress.startsWith("T")) {
            return false;
        }
        byte[] decodeCheck = Base58.decode(tronAddress);
        if (decodeCheck.length != CommonConstant.ADDRESS_SIZE + 4){
            return false;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Sm3Hash.hash(decodeData);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        if(!Arrays.equals(ArrayUtil.sub(hash1, 0, 4), ArrayUtil.sub(decodeCheck, decodeData.length, decodeData.length + 4))){
            return false;
        }
        return true;
    }

    //21字节. 前缀41(1 byte) + 以太坊地址(20 bytes)
    private static byte[] decodeFromBase58Check(String base58check) {
        Assert.isTrue(StrUtil.isNotBlank(base58check) && base58check.startsWith("T"), "address {} is invalid.", base58check);
        byte[] address = decode58Check(base58check);
        Assert.isTrue(addressValid(address));
        return address;
    }

    private static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        Assert.isTrue(decodeCheck.length == CommonConstant.ADDRESS_SIZE + 4, "decodeCheck bytes invalid");
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Sm3Hash.hash(decodeData);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        Assert.isTrue(Arrays.equals(ArrayUtil.sub(hash1, 0, 4), ArrayUtil.sub(decodeCheck, decodeData.length, decodeData.length + 4)));
        return decodeData;
    }

    public static boolean addressValid(byte[] address) {
        return ObjectUtil.isNotNull(address) && address.length == CommonConstant.ADDRESS_SIZE && address[0] == CommonConstant.ADD_PRE_FIX_BYTE_MAIN_NET;
    }

    public static String encode58Check(byte[] input) {
        Assert.notNull(input, "address invalid. input is null");
        Assert.isTrue(input.length == CommonConstant.ADDRESS_SIZE, "address invalid. input should be {} bytes, but got {} bytes", CommonConstant.ADDRESS_SIZE, input.length);
        byte[] hash0 = Sha256Sm3Hash.hash(input);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    public static String encode58Check(String input) {
        Assert.isTrue(StrUtil.isNotBlank(input), "address invalid. input is null");
        input = input.startsWith("0x") ? input.substring(2) : input;
        return encode58Check(HexUtil.decodeHex(input));
    }

    public static void main(String[] args) {
        String base58check = "TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW";
        System.out.println(WalletUtil.tronAddressHex(base58check));
        System.out.println(WalletUtil.ethAddressHex(base58check));

        String address = WalletUtil.encode58Check("0x410000000000000000000000000000000000000000");
        System.out.println(address);
    }
}
