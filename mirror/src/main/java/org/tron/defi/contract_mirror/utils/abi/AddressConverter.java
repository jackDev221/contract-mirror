package org.tron.defi.contract_mirror.utils.abi;

public class AddressConverter {
    private static final byte[] TRON_ADDRESS_PREFIX = {0X41};
    private static final int TRON_ADDRESS_BYTE_SIZE = 21;
    private static final int ETH_ADDRESS_BYTE_SIZE = 20;

    static byte[] EthToTronAddress(byte[] ethAddress) {
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

    static byte[] TronToEthAddress(byte[] tronAddress) {
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
