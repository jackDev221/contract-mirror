package org.tron.defi.contract_mirror.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tron.common.utils.AddressUtil;
import org.tron.common.utils.ByteArray;
import org.tron.sunapi.IServerConfig;
import org.tron.sunapi.SunNetwork;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "tron")
public class TronConfig implements IServerConfig {
    public static final String defaultPrivateKey
        = "000000000000000000000000000000000000000000000000000000000000001";
    private ChainConfig mainChain;
    private ChainConfig sideChain;

    private int feeLimit;
    private String privateKey;

    @Override
    public String getMainSolidityNode() {
        return mainChain == null ? null : mainChain.getSolidityNode();
    }

    @Override
    public String getMainFullNode() {
        return mainChain == null ? null : mainChain.getFullNode();
    }

    @Override
    public String getMainNetType() {
        return mainChain == null ? null : mainChain.getType();
    }

    @Override
    public int getMainRPCVersion() {
        return mainChain == null ? 0 : mainChain.getRpcVersion();
    }

    @Override
    public byte[] getMainGatewayAddress() {
        return mainChain == null ? null : mainChain.getBytesGatewayAddress();
    }

    @Override
    public String getSideSolidityNode() {
        return sideChain == null ? null : sideChain.getSolidityNode();
    }

    @Override
    public String getSideFullNode() {
        return sideChain == null ? null : sideChain.getFullNode();
    }

    @Override
    public String getSideNetType() {
        return sideChain == null ? null : sideChain.getType();
    }

    @Override
    public int getSideRPCVersion() {
        return sideChain == null ? 0 : sideChain.getRpcVersion();
    }

    @Override
    public byte[] getSideGatewayAddress() {
        return sideChain == null ? null : sideChain.getBytesGatewayAddress();
    }

    @Override
    public byte[] getSideChainId() {
        return sideChain == null ? null : sideChain.getBytesChainId();
    }

    @Bean
    public SunNetwork getSunNetwork() {
        SunNetwork sunNetwork = new SunNetwork();
        sunNetwork.init(this, null);
        String key = privateKey != null && !privateKey.isEmpty() ? privateKey : defaultPrivateKey;
        sunNetwork.setPrivateKey(key);
        return sunNetwork;
    }

    @Data
    public static class ChainConfig {
        private String chainId;
        private String type;
        private int rpcVersion;
        private String gatewayAddress;
        private List<String> fullNodeList = new ArrayList<>();
        private List<String> solidityNodeList = new ArrayList<>();

        public byte[] getBytesChainId() {
            return ByteArray.fromHexString(chainId);
        }

        public byte[] getBytesGatewayAddress() {
            return AddressUtil.decode58Check(gatewayAddress);
        }

        public String getFullNode() {
            return fullNodeList.isEmpty() ? null : fullNodeList.get(0);
        }

        public String getSolidityNode() {
            return solidityNodeList.isEmpty() ? null : solidityNodeList.get(0);
        }
    }
}
