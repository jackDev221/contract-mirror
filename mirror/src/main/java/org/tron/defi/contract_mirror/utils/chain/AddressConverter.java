package org.tron.defi.contract_mirror.utils.chain;

import org.tron.common.utils.AddressUtil;
import org.tron.common.utils.ByteArray;

public class AddressConverter {
    private static final byte[] TRON_ADDRESS_PREFIX = {0X41};
    private static final int TRON_ADDRESS_BYTE_SIZE = 21;
    private static final int ETH_ADDRESS_BYTE_SIZE = 20;

    public static String EthToTronBase58Address(String ethAddress) {
        byte[] tronAddress = EthToTronAddress(ByteArray.fromHexString(ethAddress));
        return AddressUtil.encode58Check(tronAddress);
    }

    public static String TronBase58ToEthAddress(String base58Address) {
        byte[] tronAddress = AddressUtil.decode58Check(base58Address);
        byte[] ethAddress = TronToEthAddress(tronAddress);
        return ethAddress.toString();
    }

    public static byte[] EthToTronAddress(byte[] ethAddress) {
        if (ethAddress.length < ETH_ADDRESS_BYTE_SIZE) {
            throw new IllegalArgumentException("INVALID ETH ADDRESS");
        }
        byte[] tronAddress = new byte[TRON_ADDRESS_BYTE_SIZE];
        System.arraycopy(TRON_ADDRESS_PREFIX, 0, tronAddress, 0, TRON_ADDRESS_PREFIX.length);
        System.arraycopy(ethAddress,
                         ethAddress.length - ETH_ADDRESS_BYTE_SIZE,
                         tronAddress,
                         TRON_ADDRESS_PREFIX.length,
                         ETH_ADDRESS_BYTE_SIZE);
        return tronAddress;
    }

    public static byte[] TronToEthAddress(byte[] tronAddress) {
        if (tronAddress.length != TRON_ADDRESS_BYTE_SIZE ||
            tronAddress[0] != TRON_ADDRESS_PREFIX[0]) {
            throw new IllegalArgumentException("INVALID ETH ADDRESS");
        }
        byte[] ethAddress = new byte[ETH_ADDRESS_BYTE_SIZE];
        System.arraycopy(tronAddress,
                         TRON_ADDRESS_PREFIX.length,
                         ethAddress,
                         0,
                         ETH_ADDRESS_BYTE_SIZE);
        return ethAddress;
    }
}
